$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $toolsDir = Join-Path (Split-Path $PSScriptRoot -Parent) 'herramientas'
    $maven = Join-Path $toolsDir 'apache-maven-3.10.0\bin\mvn.cmd'
    $opciones = @()
    if (Test-Path -LiteralPath $maven) {
        $opciones += "-Dmaven.repo.local=$(Join-Path $toolsDir 'maven-repository')"
    } else {
        $maven = (Get-Command mvn.cmd -ErrorAction Stop).Source
    }
    & $maven @opciones -B -ntp package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo compilar el proyecto.' }
    Write-Host 'Abre http://localhost:8086/crm/ cuando termine de iniciar.'
    Write-Host 'Para detener el servidor, presiona Ctrl+C.'
    & java.exe -jar (Join-Path $PSScriptRoot 'target\codivis-api.jar')
    if ($LASTEXITCODE -ne 0) { throw 'El servidor termino con un error.' }
} finally {
    Pop-Location
}
