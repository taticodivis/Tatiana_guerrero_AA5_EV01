param([string]$TomcatHome = $env:CATALINA_HOME)
$ErrorActionPreference = 'Stop'
if (-not $TomcatHome) { throw 'Indica -TomcatHome con la carpeta de Tomcat 10.1.' }
$tomcatDir = (Resolve-Path -LiteralPath $TomcatHome).Path
$servletJar = Join-Path $tomcatDir 'lib\servlet-api.jar'
if (-not (Test-Path -LiteralPath $servletJar)) { throw 'No se encontro lib\servlet-api.jar en Tomcat.' }
$projectDir = $PSScriptRoot
$stageDir = Join-Path $projectDir ('target\compilacion-' + [guid]::NewGuid().ToString('N'))
$classesDir = Join-Path $stageDir 'WEB-INF\classes'
New-Item -ItemType Directory -Path $classesDir -Force | Out-Null
Get-ChildItem -LiteralPath (Join-Path $projectDir 'src\main\webapp') -Force | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination $stageDir -Recurse -Force
}
$sources = @(Get-ChildItem -LiteralPath (Join-Path $projectDir 'src\main\java') -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
& javac.exe --release 17 -encoding UTF-8 -cp $servletJar -d $classesDir @sources
if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion. Usa Tomcat 10.1 con Jakarta Servlet 6.' }
$war = Join-Path $projectDir 'target\crm.war'
& jar.exe --create --file $war -C $stageDir .
if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el WAR.' }
Write-Output "WAR creado: $war"
