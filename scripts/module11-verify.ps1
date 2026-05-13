param()

$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host "[MODULE11] $Message"
}

function Fail {
    param([string]$Message)
    throw "[MODULE11 VERIFY FAILED] $Message"
}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        Fail $Message
    }
}

function Read-Utf8 {
    param([string]$Path)
    return [System.IO.File]::ReadAllText($Path, [Text.UTF8Encoding]::new($false))
}

function Assert-Contains {
    param(
        [string]$Content,
        [string]$Term,
        [string]$Path
    )

    Assert-True ($Content.Contains($Term)) "$Path does not contain required term: $Term"
}

$root = (Get-Location).Path

if (-not (Test-Path -LiteralPath (Join-Path $root "docker-compose.yml") -PathType Leaf)) {
    Fail "Please run this script from the Secure Vault AI project root."
}

$requiredFiles = @(
    "scripts/start-vault.ps1",
    "scripts/stop-vault.ps1",
    "scripts/logs-vault.ps1",
    "scripts/module11-verify.ps1",
    "backend/src/main/resources/static/index.html",
    "backend/src/main/resources/static/app.js",
    "backend/src/main/resources/static/styles.css",
    "docs/user-guide.md"
)

Write-Step "Check required Module 11 files"

foreach ($file in $requiredFiles) {
    Assert-True (Test-Path -LiteralPath (Join-Path $root $file) -PathType Leaf) "Missing required file: $file"
}

$indexHtml = Read-Utf8 (Join-Path $root "backend/src/main/resources/static/index.html")
$appJs = Read-Utf8 (Join-Path $root "backend/src/main/resources/static/app.js")
$stylesCss = Read-Utf8 (Join-Path $root "backend/src/main/resources/static/styles.css")
$startScript = Read-Utf8 (Join-Path $root "scripts/start-vault.ps1")
$readme = Read-Utf8 (Join-Path $root "README.md")
$userGuide = Read-Utf8 (Join-Path $root "docs/user-guide.md")
$termChinese = -join ([char[]](0x4E2D, 0x6587))
$termLanguageSwitch = -join ([char[]](0x4E2D, 0x82F1, 0x6587, 0x5207, 0x6362))

Write-Step "Check index.html terms"

foreach ($term in @("Secure Vault AI", "data-i18n", "data-i18n-placeholder", "language", $termChinese, "English", "languageSelect", "app.js", "styles.css")) {
    Assert-Contains -Content $indexHtml -Term $term -Path "index.html"
}

foreach ($term in @("sidebar-header", "sidebar-body", "sidebar-footer", "sidebar-section", "section-scroll", "conversation-scroll", "document-scroll")) {
    Assert-True (($indexHtml.Contains($term)) -or ($stylesCss.Contains($term))) "index.html or styles.css does not contain required layout term: $term"
}

Write-Step "Check app.js terms"

foreach ($term in @("localStorage", "Authorization", "Bearer", "FormData", "fetch", "email", "apiFetch", "renderSources", "logout", "ask", "contentPreview", "No readable preview was returned", "source.documentId", "I18N", "zh-CN", "en-US", "secureVaultLanguage", "applyLanguage", "setLanguage", "data-i18n", "data-i18n-placeholder")) {
    Assert-Contains -Content $appJs -Term $term -Path "app.js"
}

Assert-True (-not $appJs.Contains("source.chunkId")) "app.js must not use chunkId as a source preview fallback"

Write-Step "Check styles.css terms"

foreach ($term in @("sidebar", "chat", "composer", "message", "source", "badge", "language")) {
    Assert-Contains -Content $stylesCss -Term $term -Path "styles.css"
}

foreach ($term in @("min-height: 0", "overflow-y: auto", "flex-direction: column", "100vh")) {
    Assert-Contains -Content $stylesCss -Term $term -Path "styles.css"
}

Write-Step "Check start-vault.ps1 terms"

foreach ($term in @("docker compose up -d --build", "Start-Process", "JWT_SECRET", "FILE_ENCRYPTION_KEY", "RandomNumberGenerator")) {
    Assert-Contains -Content $startScript -Term $term -Path "start-vault.ps1"
}

Write-Step "Check README terms"

foreach ($term in @("Quick Start for Personal Use", "scripts/start-vault.ps1", "docs/user-guide.md", "http://localhost:8080")) {
    Assert-Contains -Content $readme -Term $term -Path "README.md"
}

Write-Step "Check user guide terms"

foreach ($term in @("start-vault.ps1", "stop-vault.ps1", "logs-vault.ps1", "Register", "Login", "Upload", "Embed", "Ask", "sources", "audit logs", $termLanguageSwitch, "English", $termChinese)) {
    Assert-Contains -Content $userGuide -Term $term -Path "docs/user-guide.md"
}

Write-Step "Run safety checks"

$safetyFiles = @(
    "README.md",
    "docs/user-guide.md",
    "docs/demo-script.md",
    "scripts/start-vault.ps1",
    "scripts/stop-vault.ps1",
    "scripts/logs-vault.ps1",
    "backend/src/main/resources/static/index.html",
    "backend/src/main/resources/static/app.js",
    "backend/src/main/resources/static/styles.css"
)

$combined = ""
foreach ($file in $safetyFiles) {
    if (Test-Path -LiteralPath (Join-Path $root $file) -PathType Leaf) {
        $combined += "`n--- FILE: $file ---`n"
        $combined += Read-Utf8 (Join-Path $root $file)
    }
}

$unfinishedWords = @("TO" + "DO", "TB" + "D", "FIX" + "ME")
$escapedUnfinishedWords = $unfinishedWords | ForEach-Object { [regex]::Escape($_) }
$unfinishedPattern = '(?i)\b(' + ($escapedUnfinishedWords -join "|") + ')\b'
Assert-True (-not ($combined -match $unfinishedPattern)) "Module 11 files contain unfinished placeholder words"
Assert-True (-not ($combined -match 'Authorization:\s*Bearer\s+eyJ[A-Za-z0-9_\-]+')) "Module 11 files contain a JWT-looking Authorization header"
Assert-True (-not ($combined -match '\bBearer\s+eyJ[A-Za-z0-9_\-]+')) "Module 11 files contain a JWT-looking bearer token sample"
Assert-True (-not ($combined -match '(?i)C:\\Users\\')) "Module 11 files contain a real-looking Windows user directory"

$jwtSecretMatches = [regex]::Matches($combined, '(?im)^\s*JWT_SECRET\s*=\s*([^\s`"'']+)')
foreach ($match in $jwtSecretMatches) {
    $value = $match.Groups[1].Value.Trim()
    $allowed = @("replace-with-at-least-64-bytes-secret", "change-me-generated-by-setup")

    if ($allowed -notcontains $value -and $value.Length -ge 32) {
        Fail "Module 11 files contain a suspicious JWT_SECRET value"
    }
}

$fileKeyMatches = [regex]::Matches($combined, '(?im)^\s*FILE_ENCRYPTION_KEY\s*=\s*([^\s`"'']+)')
foreach ($match in $fileKeyMatches) {
    $value = $match.Groups[1].Value.Trim()
    $allowed = @("replace-with-base64-32-byte-key", "replace-with-base64-32-byte-key-placeholder")

    if ($allowed -notcontains $value -and $value.Length -ge 32) {
        Fail "Module 11 files contain a suspicious FILE_ENCRYPTION_KEY value"
    }
}

$devKeyPath = Join-Path $root ".secure-vault/file-encryption.key"
if (Test-Path -LiteralPath $devKeyPath -PathType Leaf) {
    $devKeyContent = (Read-Utf8 $devKeyPath).Trim()
    if ($devKeyContent.Length -ge 16) {
        Assert-True (-not $combined.Contains($devKeyContent)) "Module 11 files contain the local file-encryption.key content"
    }
}

foreach ($placeholder in @("<token>", "<documentId>", "<auditLogId>", "replace-with-at-least-64-bytes-secret", "replace-with-base64-32-byte-key")) {
    if ($combined.Contains($placeholder)) {
        Write-Step "Allowed placeholder found: $placeholder"
    }
}

Write-Host "MODULE 11 USABILITY VERIFY PASSED"
