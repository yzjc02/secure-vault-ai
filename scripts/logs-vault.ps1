param()

$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath "docker-compose.yml" -PathType Leaf)) {
    throw "Please run this script from the Secure Vault AI project root."
}

docker compose logs -f backend
