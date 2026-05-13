param()

$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host "[MODULE10] $Message"
}

function Fail {
    param([string]$Message)
    throw "[MODULE10 VERIFY FAILED] $Message"
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
    return Get-Content -Raw -Encoding UTF8 $Path
}

$root = (Get-Location).Path
$requiredFiles = @(
    "README.md",
    "docs/architecture.md",
    "docs/api-guide.md",
    "docs/demo-script.md",
    "docs/resume-points.md",
    "docs/module10-release-checklist.md"
)

Write-Step "Check required documentation files"

foreach ($file in $requiredFiles) {
    Assert-True (Test-Path -LiteralPath (Join-Path $root $file) -PathType Leaf) "Missing required file: $file"
}

$readme = Read-Utf8 (Join-Path $root "README.md")
$readmeRequiredTerms = @(
    "Secure Vault AI",
    "Docker Compose",
    "RAG",
    "pgvector",
    "JWT",
    "audit",
    "docs/architecture.md",
    "docs/api-guide.md",
    "docs/demo-script.md",
    "docs/resume-points.md"
)

Write-Step "Check README required terms"

foreach ($term in $readmeRequiredTerms) {
    Assert-True ($readme.Contains($term)) "README.md does not contain required term: $term"
}

Write-Step "Check registration examples include email"

$apiGuide = Read-Utf8 (Join-Path $root "docs/api-guide.md")
$demoScript = Read-Utf8 (Join-Path $root "docs/demo-script.md")

Assert-True ($apiGuide -match '(?s)register.*email|email.*register') "docs/api-guide.md registration example must include email"
Assert-True ($demoScript -match '(?s)register.*email|email.*register') "docs/demo-script.md registration example must include email"

Write-Step "Collect markdown files for static safety checks"

$markdownFiles = @()
$markdownFiles += Get-Item -LiteralPath (Join-Path $root "README.md")
$markdownFiles += Get-ChildItem -LiteralPath (Join-Path $root "docs") -Filter "*.md" -File

$combinedMarkdown = ""
foreach ($file in $markdownFiles) {
    $combinedMarkdown += "`n--- FILE: $($file.Name) ---`n"
    $combinedMarkdown += Read-Utf8 $file.FullName
}

Write-Step "Check unfinished placeholders"

Assert-True (-not ($combinedMarkdown -match '(?i)\b(TODO|TBD|FIXME)\b')) "Markdown files contain unfinished placeholder words: TODO, TBD, or FIXME"

Write-Step "Check JWT and encryption key examples are placeholders"

Assert-True (-not ($combinedMarkdown -match 'Authorization:\s*Bearer\s+eyJ[A-Za-z0-9_\-]+')) "Markdown files contain a JWT-looking Authorization header"

$jwtSecretMatches = [regex]::Matches($combinedMarkdown, '(?im)^\s*JWT_SECRET\s*=\s*([^\s`"'']+)')
foreach ($match in $jwtSecretMatches) {
    $value = $match.Groups[1].Value.Trim()
    $allowed = @(
        "replace-with-at-least-64-bytes-secret"
    )

    if ($allowed -notcontains $value -and $value.Length -ge 32) {
        Fail "Markdown files contain a suspicious JWT_SECRET value"
    }
}

$fileKeyMatches = [regex]::Matches($combinedMarkdown, '(?im)^\s*FILE_ENCRYPTION_KEY\s*=\s*([^\s`"'']+)')
foreach ($match in $fileKeyMatches) {
    $value = $match.Groups[1].Value.Trim()
    $allowed = @(
        "replace-with-base64-32-byte-key"
    )

    if ($allowed -notcontains $value -and $value.Length -ge 32) {
        Fail "Markdown files contain a suspicious FILE_ENCRYPTION_KEY value"
    }
}

Write-Step "Check local privacy paths and key file content"

Assert-True (-not ($combinedMarkdown -match '(?i)C:\\Users\\')) "Markdown files contain a real-looking Windows user directory"

$devKeyPath = Join-Path $root ".secure-vault/file-encryption.key"
if (Test-Path -LiteralPath $devKeyPath -PathType Leaf) {
    $devKeyContent = (Read-Utf8 $devKeyPath).Trim()
    if ($devKeyContent.Length -ge 16) {
        Assert-True (-not $combinedMarkdown.Contains($devKeyContent)) "Markdown files contain the local file-encryption.key content"
    }
}

Write-Step "Check allowed placeholders are present or permitted"

$allowedPlaceholders = @(
    "<token>",
    "<documentId>",
    "<auditLogId>",
    "replace-with-at-least-64-bytes-secret",
    "replace-with-base64-32-byte-key"
)

foreach ($placeholder in $allowedPlaceholders) {
    if ($combinedMarkdown.Contains($placeholder)) {
        Write-Step "Allowed placeholder found: $placeholder"
    }
}

Write-Step "PASS: required files, docs terms, examples, and safety checks are valid"
Write-Host "MODULE 10 DOCUMENTATION VERIFY PASSED"
