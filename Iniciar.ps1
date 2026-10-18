param(
    [string]$TomcatHome,
    [ValidateRange(1024, 65535)][int]$Puerto = 8086,
    [switch]$SoloPreparar
)
$ErrorActionPreference = 'Stop'
$projectDir = $PSScriptRoot

# Mantiene un unico servidor en el puerto del proyecto.
$sonda = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $Puerto)
try { $sonda.Start() } catch { throw "No se pudo liberar el puerto $Puerto. Cierra la ejecucion anterior del proyecto." } finally { $sonda.Stop() }

# Encuentra las herramientas que ya existen junto al proyecto.
if (-not $TomcatHome) {
    $toolsDir = Join-Path (Split-Path $projectDir -Parent) 'herramientas'
    $tomcat = Get-ChildItem -LiteralPath $toolsDir -Directory -Filter 'apache-tomcat-10.1*' -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($tomcat) {
        $TomcatHome = $tomcat.FullName
    } elseif ($env:CATALINA_HOME -and (Test-Path -LiteralPath (Join-Path $env:CATALINA_HOME 'lib\servlet-api.jar'))) {
        $TomcatHome = $env:CATALINA_HOME
    } else {
        [xml]$pom = Get-Content -LiteralPath (Join-Path $projectDir 'pom.xml') -Raw
        $version = [string]$pom.project.properties.'tomcat.reference.version'
        $TomcatHome = & (Join-Path $projectDir 'Obtener-Tomcat.ps1') -Version $version
    }
}
$tomcatDir = (Resolve-Path -LiteralPath $TomcatHome).Path
$javac = Get-Command javac.exe -ErrorAction SilentlyContinue
if (-not $javac -and $env:JAVA_HOME) {
    $jdkBin = Join-Path $env:JAVA_HOME 'bin'
    if (Test-Path -LiteralPath (Join-Path $jdkBin 'javac.exe')) {
        $env:Path = $jdkBin + ';' + $env:Path
        $javac = Get-Command javac.exe -ErrorAction SilentlyContinue
    }
}
if (-not $javac) { throw 'No se encontro el JDK. Agrega su carpeta bin al PATH.' }
$env:JAVA_HOME = Split-Path (Split-Path $javac.Source)

& (Join-Path $projectDir 'Compilar.ps1') -TomcatHome $tomcatDir

# La instalacion de Tomcat se reutiliza; los archivos de ejecucion quedan en target.
$runtimeDir = Join-Path $projectDir ('target\tomcat-' + $Puerto)
foreach ($directory in @('conf', 'logs', 'temp', 'webapps', 'work')) {
    New-Item -ItemType Directory -Path (Join-Path $runtimeDir $directory) -Force | Out-Null
}
Get-ChildItem -LiteralPath (Join-Path $tomcatDir 'conf') -File | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $runtimeDir 'conf') -Force
}
$serverXml = @"
<?xml version="1.0" encoding="UTF-8"?>
<Server port="-1">
  <Service name="Catalina">
    <Connector port="$Puerto" address="127.0.0.1" protocol="org.apache.coyote.http11.Http11NioProtocol" connectionTimeout="20000" />
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" unpackWARs="false" autoDeploy="false" deployOnStartup="true">
        <Valve className="org.apache.catalina.valves.ErrorReportValve" showReport="false" showServerInfo="false" />
      </Host>
    </Engine>
  </Service>
</Server>
"@
[IO.File]::WriteAllText((Join-Path $runtimeDir 'conf\server.xml'), $serverXml, [Text.UTF8Encoding]::new($false))
Copy-Item -LiteralPath (Join-Path $projectDir 'target\crm.war') -Destination (Join-Path $runtimeDir 'webapps\crm.war') -Force
$env:CATALINA_HOME = $tomcatDir
$env:CATALINA_BASE = $runtimeDir
$env:CATALINA_TMPDIR = Join-Path $runtimeDir 'temp'
# Mantiene los clientes fuera de target, para conservarlos al limpiar la compilacion.
$dataDir = Join-Path $projectDir 'data'
$env:CATALINA_OPTS = '-Dcrm.data.dir="' + $dataDir + '"'
$configRunner = @"
tomcat.home=$($tomcatDir.Replace('\', '/'))
tomcat.base=$($runtimeDir.Replace('\', '/'))
data.dir=$($dataDir.Replace('\', '/'))
port=$Puerto
"@
[IO.File]::WriteAllText((Join-Path $projectDir 'target\runner.properties'), $configRunner, [Text.UTF8Encoding]::new($false))
if ($SoloPreparar) { return }
Write-Host "Abre http://localhost:$Puerto/crm/ cuando Tomcat termine de iniciar."
Write-Host 'Para detener el proyecto, presiona Ctrl+C en esta terminal.'
& (Join-Path $tomcatDir 'bin\catalina.bat') run
if ($LASTEXITCODE -ne 0) { throw 'Tomcat no pudo iniciar. Revisa el mensaje en la terminal.' }
