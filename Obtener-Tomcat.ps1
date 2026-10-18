param(
    [Parameter(Mandatory = $true)][string]$Version,
    [string]$CacheDir = (Join-Path $PSScriptRoot 'target\herramientas')
)
$ErrorActionPreference = 'Stop'
if ($Version -notmatch '^10\.1\.\d+$') { throw 'La version de Tomcat debe ser 10.1.x.' }
$installationDir = Join-Path $CacheDir ('apache-tomcat-' + $Version)
if ((Test-Path -LiteralPath (Join-Path $installationDir 'bin\bootstrap.jar')) -and
    (Test-Path -LiteralPath (Join-Path $installationDir 'lib\servlet-api.jar'))) {
    Write-Output (Resolve-Path -LiteralPath $installationDir).Path
    return
}

New-Item -ItemType Directory -Path $CacheDir -Force | Out-Null
$archiveName = "apache-tomcat-$Version.zip"
$archivePath = Join-Path $CacheDir $archiveName
$sources = @(
    "https://dlcdn.apache.org/tomcat/tomcat-10/v$Version/bin",
    "https://archive.apache.org/dist/tomcat/tomcat-10/v$Version/bin"
)
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$downloaded = $false
foreach ($source in $sources) {
    try {
        Write-Host "Primera ejecucion: descargando Tomcat $Version desde Apache..."
        $checksum = (Invoke-WebRequest -Uri "$source/$archiveName.sha512" -UseBasicParsing -TimeoutSec 30).Content
        if ($checksum -is [byte[]]) { $checksum = [Text.Encoding]::UTF8.GetString($checksum) }
        $expected = [regex]::Match([string]$checksum, '(?i)\b[0-9a-f]{128}\b').Value
        if (-not $expected) { throw 'Apache no devolvio una suma SHA512 valida.' }
        Invoke-WebRequest -Uri "$source/$archiveName" -OutFile $archivePath -UseBasicParsing -TimeoutSec 120
        if ((Get-FileHash -LiteralPath $archivePath -Algorithm SHA512).Hash -ne $expected) {
            throw 'La descarga no coincide con la suma SHA512 publicada por Apache.'
        }
        $downloaded = $true
        break
    } catch {
        Write-Host "No se pudo descargar desde este servidor: $($_.Exception.Message)"
    }
}
if (-not $downloaded) { throw 'No se pudo descargar Tomcat. Revisa la conexion a Internet o indica -TomcatHome con una instalacion existente.' }
Expand-Archive -LiteralPath $archivePath -DestinationPath $CacheDir -Force
if (-not (Test-Path -LiteralPath (Join-Path $installationDir 'lib\servlet-api.jar'))) {
    throw 'La instalacion descargada de Tomcat esta incompleta.'
}
Write-Output (Resolve-Path -LiteralPath $installationDir).Path
