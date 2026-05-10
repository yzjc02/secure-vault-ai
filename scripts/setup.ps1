$ErrorActionPreference = "Stop"
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)

$rootDir = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $rootDir ".env"

function New-RandomSecret {
    param(
        [Parameter(Mandatory = $true)]
        [int] $ByteCount,
        [Parameter(Mandatory = $true)]
        [int] $Length
    )

    $bytes = [byte[]]::new($ByteCount)
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
    } finally {
        $rng.Dispose()
    }
    $secret = [Convert]::ToBase64String($bytes).Replace("+", "-").Replace("/", "_").Replace("=", "")
    return $secret.Substring(0, $Length)
}

if (Test-Path -LiteralPath $envPath) {
    Write-Host ".env already exists. Keeping existing local configuration."
    Write-Host "Next step:"
    Write-Host "  docker compose up -d"
    exit 0
}

$jwtSecret = New-RandomSecret -ByteCount 72 -Length 96
$postgresPassword = New-RandomSecret -ByteCount 36 -Length 48

$content = @"
APP_PORT=8080
SPRING_PROFILES_ACTIVE=prod
POSTGRES_DB=securevault
POSTGRES_USER=securevault_user
POSTGRES_PASSWORD=$postgresPassword
JWT_SECRET=$jwtSecret
JWT_EXPIRATION=86400000
FILE_STORAGE_DIR=/app/data/uploads
MAX_FILE_SIZE=20971520
"@

[System.IO.File]::WriteAllText($envPath, $content + [Environment]::NewLine, [Text.UTF8Encoding]::new($false))

Write-Host ".env created with local secrets."
Write-Host "Next step:"
Write-Host "  docker compose up -d"
