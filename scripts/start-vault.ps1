param()

$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$ErrorActionPreference = "Stop"

function Assert-ProjectRoot {
    if (-not (Test-Path -LiteralPath "docker-compose.yml" -PathType Leaf) -or
        -not (Test-Path -LiteralPath "backend" -PathType Container) -or
        -not (Test-Path -LiteralPath ".env.example" -PathType Leaf)) {
        throw "Please run this script from the Secure Vault AI project root."
    }
}

function New-Base64Secret {
    param([int]$ByteCount)

    $bytes = [byte[]]::new($ByteCount)
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()

    try {
        $rng.GetBytes($bytes)
        return [Convert]::ToBase64String($bytes)
    } finally {
        $rng.Dispose()
    }
}

function Get-EnvValue {
    param(
        [string[]]$Lines,
        [string]$Name
    )

    foreach ($line in $Lines) {
        if ($line -match "^\s*$([regex]::Escape($Name))=(.*)$") {
            return $Matches[1].Trim()
        }
    }

    return $null
}

function Set-EnvValue {
    param(
        [string[]]$Lines,
        [string]$Name,
        [string]$Value
    )

    $updated = New-Object System.Collections.Generic.List[string]
    $found = $false

    foreach ($line in $Lines) {
        if ($line -match "^\s*$([regex]::Escape($Name))=") {
            $updated.Add("$Name=$Value")
            $found = $true
        } else {
            $updated.Add($line)
        }
    }

    if (-not $found) {
        $updated.Add("$Name=$Value")
    }

    return $updated.ToArray()
}

function Test-ObviousPlaceholder {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $true
    }

    return $Value -match '^(change-me|changeme|replace-with|password|postgres|secret|example|demo)'
}

function Test-DockerAvailable {
    try {
        docker version | Out-Null
        docker compose version | Out-Null
    } catch {
        Write-Host "Docker Desktop is not running. Please start Docker Desktop and retry."
        exit 1
    }
}

function Initialize-EnvFile {
    $envPath = Join-Path (Get-Location).Path ".env"

    if (Test-Path -LiteralPath $envPath -PathType Leaf) {
        return
    }

    Copy-Item -LiteralPath ".env.example" -Destination $envPath

    $utf8NoBom = [Text.UTF8Encoding]::new($false)
    [string[]]$lines = [System.IO.File]::ReadAllLines($envPath, $utf8NoBom)

    $lines = Set-EnvValue -Lines $lines -Name "JWT_SECRET" -Value (New-Base64Secret 64)
    $lines = Set-EnvValue -Lines $lines -Name "FILE_ENCRYPTION_KEY" -Value (New-Base64Secret 32)

    $postgresPassword = Get-EnvValue -Lines $lines -Name "POSTGRES_PASSWORD"
    if (Test-ObviousPlaceholder $postgresPassword) {
        $lines = Set-EnvValue -Lines $lines -Name "POSTGRES_PASSWORD" -Value (New-Base64Secret 32)
    }

    [System.IO.File]::WriteAllLines($envPath, $lines, $utf8NoBom)
    Write-Host "Created local .env from .env.example."
}

function Wait-StaticHome {
    $deadline = (Get-Date).AddSeconds(90)

    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Method Get -Uri "http://localhost:8080/" -UseBasicParsing -TimeoutSec 5
            if ([int]$response.StatusCode -ge 200 -and [int]$response.StatusCode -lt 400) {
                return
            }
        } catch {
            Start-Sleep -Seconds 3
        }
    }

    throw "Backend did not become reachable at http://localhost:8080/ within 90 seconds. Run scripts/logs-vault.ps1 for details."
}

Assert-ProjectRoot
Test-DockerAvailable
Initialize-EnvFile

docker compose up -d --build

Wait-StaticHome

Start-Process "http://localhost:8080"

Write-Host "SECURE VAULT AI STARTED"
Write-Host "Open: http://localhost:8080"
