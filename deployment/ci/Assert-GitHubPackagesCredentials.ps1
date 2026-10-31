$ErrorActionPreference = 'Stop'

foreach ($name in @('GITHUB_PACKAGES_USER', 'GITHUB_PACKAGES_TOKEN')) {
    $value = [Environment]::GetEnvironmentVariable($name)
    if ([string]::IsNullOrWhiteSpace($value) -or $value -match '^\$\([^)]+\)$') {
        throw "Configura $name en las variables autorizadas del pipeline. GITHUB_PACKAGES_TOKEN debe ser secreta y tener permiso read:packages sobre el starter PEP. No copies su valor al YAML ni a los logs."
    }
}
