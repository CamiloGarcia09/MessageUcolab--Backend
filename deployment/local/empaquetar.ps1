# Arma messageucolab-local.zip: lo que necesita un equipo para levantar MessageUcoLab sin el código.
# Toma compose.yaml, .env y README.md de esta carpeta y la configuración de ../docker, para que cada
# archivo tenga un solo original en el repositorio.
# Uso: .\empaquetar.ps1 [-Destino <carpeta>]
param([string]$Destino = (Join-Path $PSScriptRoot 'dist'))

$ErrorActionPreference = 'Stop'
$docker = Join-Path $PSScriptRoot '..\docker'
$carpeta = Join-Path $Destino 'messageucolab-local'
$zip = Join-Path $Destino 'messageucolab-local.zip'

if (Test-Path $carpeta) { Remove-Item $carpeta -Recurse -Force }
New-Item -ItemType Directory -Force (Join-Path $carpeta 'scripts') | Out-Null

foreach ($archivo in 'compose.yaml', '.env', 'README.md') {
  Copy-Item (Join-Path $PSScriptRoot $archivo) $carpeta
}
foreach ($archivo in 'kong.yaml', 'loki-config.yaml', 'otel-collector-config.yaml', 'prometheus.yaml') {
  Copy-Item (Join-Path $docker $archivo) $carpeta
}
Copy-Item (Join-Path $docker 'scripts\redis') (Join-Path $carpeta 'scripts') -Recurse
Copy-Item (Join-Path $docker 'scripts\surreal') (Join-Path $carpeta 'scripts') -Recurse

if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path $carpeta -DestinationPath $zip
"Paquete listo: $zip"
