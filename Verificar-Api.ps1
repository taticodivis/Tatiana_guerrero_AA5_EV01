param([string]$BaseUrl = 'http://localhost:8086/crm/api')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$coleccion = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'postman\Codivis-AA5-EV01.postman_collection.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$idPrueba = [guid]::NewGuid().ToString('N').Substring(0,20)
$variables = @{ baseUrl=$BaseUrl; usuario="prueba_$idPrueba"; otro="otro_$idPrueba"; inexistente="no_$idPrueba"; clave='PruebaCodivis2026!' }
function Expandir([string]$texto) {
    foreach ($nombre in $variables.Keys) { $texto = $texto.Replace(('{{' + $nombre + '}}'), $variables[$nombre]) }
    return $texto
}
$cliente = [Net.Http.HttpClient]::new()
$cliente.Timeout = [TimeSpan]::FromSeconds(30)
$resultados = @()
try {
    foreach ($caso in $coleccion.item) {
        $solicitud = [Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::new($caso.request.method), (Expandir $caso.request.url))
        if ($caso.request.body.mode -eq 'urlencoded') {
            $pares = [Collections.Generic.List[Collections.Generic.KeyValuePair[string,string]]]::new()
            foreach ($campo in $caso.request.body.urlencoded) {
                $pares.Add([Collections.Generic.KeyValuePair[string,string]]::new($campo.key, (Expandir $campo.value)))
            }
            $solicitud.Content = [Net.Http.FormUrlEncodedContent]::new($pares)
        } elseif ($caso.request.body.mode -eq 'raw') {
            $solicitud.Content = [Net.Http.StringContent]::new($caso.request.body.raw, [Text.Encoding]::UTF8, 'application/json')
        }
        $respuesta = $cliente.SendAsync($solicitud).GetAwaiter().GetResult()
        $cuerpo = $respuesta.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $datos = $cuerpo | ConvertFrom-Json
        $scriptTest = $caso.event[0].script.exec -join "`n"
        $esperado = [int][regex]::Match($scriptTest, 'to.have.status\((\d+)\)').Groups[1].Value
        $codigo = [regex]::Match($scriptTest, 'data.codigo\).to.eql\("([A-Z_]+)"\)').Groups[1].Value
        $correcto = [int]$respuesta.StatusCode -eq $esperado -and $datos.codigo -eq $codigo -and $datos.ok -eq ($esperado -lt 300) -and $datos.mensaje -is [string] -and $datos.mensaje.Length -gt 0 -and $respuesta.Content.Headers.ContentType.MediaType -eq 'application/json' -and -not ($datos.PSObject.Properties.Name -contains 'clave') -and -not ($datos.PSObject.Properties.Name -contains 'hash')
        $resultados += [pscustomobject]@{ prueba=$caso.name; esperado=$esperado; recibido=[int]$respuesta.StatusCode; codigo=$datos.codigo; correcto=$correcto; respuesta=$cuerpo }
        Write-Host "$($caso.name): HTTP $([int]$respuesta.StatusCode) - $correcto"
        $respuesta.Dispose()
        $solicitud.Dispose()
    }
} finally { $cliente.Dispose() }
$salida = Join-Path $PSScriptRoot 'evidencias\resultado-http.json'
[pscustomobject]@{ fecha=(Get-Date).ToString('o'); herramienta='PowerShell HttpClient (comprobacion previa; pendiente ejecutar en Postman)'; baseUrl=$BaseUrl; resultados=$resultados } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $salida -Encoding UTF8
if (@($resultados | Where-Object { -not $_.correcto }).Count -gt 0) { throw 'Hay pruebas fallidas. Revisa evidencias/resultado-http.json.' }
Write-Host "$($resultados.Count) casos HTTP correctos. Falta ejecutarlos en Postman para la evidencia."
