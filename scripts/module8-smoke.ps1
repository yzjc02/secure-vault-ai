param(
    [string]$BaseUrl = "http://localhost:8080"
)

Add-Type -AssemblyName System.Net.Http

$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host "[MODULE8] $Message"
}

function Assert-True {
    param(
        [object]$Condition,
        [string]$Message
    )

    if (-not [bool]$Condition) {
        throw $Message
    }
}

function Convert-ToJsonBody {
    param([hashtable]$Body)

    # Important:
    # Return a JSON string, not UTF-8 byte[].
    # Windows PowerShell Invoke-WebRequest can send byte[] in a way that Spring treats as malformed JSON.
    return ($Body | ConvertTo-Json -Compress -Depth 10)
}

function Invoke-Json {
    param(
        [string]$Method,
        [string]$Path,
        [hashtable]$Body,
        [string]$Token
    )

    $headers = @{}

    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $requestParams = @{
        Method      = $Method
        Uri         = "$BaseUrl$Path"
        Headers     = $headers
        ContentType = "application/json; charset=utf-8"
    }

    if ($null -ne $Body) {
        $requestParams["Body"] = Convert-ToJsonBody $Body
    }

    try {
        $response = Invoke-WebRequest @requestParams
        $raw = [string]$response.Content

        return @{
            StatusCode = [int]$response.StatusCode
            Raw        = $raw
            Json       = ($raw | ConvertFrom-Json)
        }
    } catch [System.Net.WebException] {
        $httpResponse = $_.Exception.Response

        if ($null -eq $httpResponse) {
            throw
        }

        $reader = New-Object System.IO.StreamReader($httpResponse.GetResponseStream(), [System.Text.Encoding]::UTF8)
        $raw = $reader.ReadToEnd()

        $json = $null
        if (-not [string]::IsNullOrWhiteSpace($raw)) {
            $json = ($raw | ConvertFrom-Json)
        }

        return @{
            StatusCode = [int]$httpResponse.StatusCode
            Raw        = $raw
            Json       = $json
        }
    }
}

function Invoke-Upload {
    param(
        [string]$Token,
        [string]$FileName,
        [string]$Content
    )

    $client = New-Object System.Net.Http.HttpClient

    try {
        $client.DefaultRequestHeaders.Authorization =
            New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $Token)

        $form = New-Object System.Net.Http.MultipartFormDataContent

        $bytes = [System.Text.Encoding]::UTF8.GetBytes($Content)
        $fileContent = New-Object System.Net.Http.ByteArrayContent(,$bytes)
        $fileContent.Headers.ContentType =
            New-Object System.Net.Http.Headers.MediaTypeHeaderValue("text/plain")

        $form.Add($fileContent, "file", $FileName)

        $response = $client.PostAsync("$BaseUrl/api/documents/upload", $form).Result
        $raw = $response.Content.ReadAsStringAsync().Result

        $json = $null
        if (-not [string]::IsNullOrWhiteSpace($raw)) {
            $json = ($raw | ConvertFrom-Json)
        }

        return @{
            StatusCode = [int]$response.StatusCode
            Raw        = $raw
            Json       = $json
        }
    } finally {
        if ($form) {
            $form.Dispose()
        }

        if ($client) {
            $client.Dispose()
        }
    }
}

function Register-AndLogin {
    param([string]$Prefix)

    $suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $username = "$Prefix$suffix"
    $email = "$username@example.com"
    $password = "Password123"

    Write-Step "Register $username"

    $register = Invoke-Json -Method "POST" -Path "/api/auth/register" -Body @{
        username = $username
        email    = $email
        password = $password
    }

    Assert-True `
        ($register.StatusCode -eq 200 -and $register.Json.code -eq 0) `
        "Register failed for $username. HTTP=$($register.StatusCode), Raw=$($register.Raw)"

    Write-Step "Login $username"

    $login = Invoke-Json -Method "POST" -Path "/api/auth/login" -Body @{
        username = $username
        password = $password
    }

    Assert-True `
        ($login.StatusCode -eq 200 -and $login.Json.code -eq 0) `
        "Login failed for $username. HTTP=$($login.StatusCode), Raw=$($login.Raw)"

    Assert-True `
        (-not [string]::IsNullOrWhiteSpace([string]$login.Json.data.token)) `
        "Login response did not include token. Raw=$($login.Raw)"

    return $login.Json.data.token
}

function Assert-Status {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token,
        [int]$ExpectedStatus
    )

    $response = Invoke-Json -Method $Method -Path $Path -Token $Token

    Assert-True `
        ($response.StatusCode -eq $ExpectedStatus) `
        "$Method $Path expected $ExpectedStatus but got $($response.StatusCode). Raw=$($response.Raw)"
}

function Assert-NoSensitiveFields {
    param([string]$Raw)

    $blocked = @(
        "embedding",
        "filePath",
        "storedFilename",
        "userId",
        "fullPrompt",
        "encryptionKey",
        "jdbc:",
        "Bearer ",
        "C:\",
        "/uploads/"
    )

    foreach ($item in $blocked) {
        Assert-True `
            (-not $Raw.Contains($item)) `
            "Response leaked sensitive token: $item. Raw=$Raw"
    }
}

function Assert-NoPlaintextInUploadStorage {
    param([string]$Needle)

    $checked = $false
    $root = Split-Path -Parent $PSScriptRoot

    $candidateDirs = @(
        (Join-Path $root "backend/data/uploads"),
        (Join-Path $root "data/uploads"),
        (Join-Path $root "uploads")
    )

    foreach ($dir in $candidateDirs) {
        if (Test-Path $dir) {
            $checked = $true

            Get-ChildItem -Path $dir -Recurse -File | ForEach-Object {
                $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
                $text = [System.Text.Encoding]::GetEncoding("ISO-8859-1").GetString($bytes)

                Assert-True `
                    (-not $text.Contains($Needle)) `
                    "Upload storage contains plaintext secret in local storage"
            }
        }
    }

    $docker = Get-Command docker -ErrorAction SilentlyContinue

    if ($docker) {
        Push-Location $root

        try {
            $composeCheck = & docker compose ps --services 2>$null

            if ($LASTEXITCODE -eq 0 -and ($composeCheck -contains "backend")) {
                $checked = $true

                & docker compose exec -T backend sh -c "if grep -R -q '$Needle' /app/data/uploads 2>/dev/null; then exit 2; else exit 0; fi"

                Assert-True `
                    ($LASTEXITCODE -eq 0) `
                    "Docker upload storage contains plaintext secret"
            }
        } finally {
            Pop-Location
        }
    }

    Assert-True $checked "Could not inspect upload storage"
}

Write-Step "Register and login users"

$tokenA = Register-AndLogin "module8a"
$tokenB = Register-AndLogin "module8b"

$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$secret = "MODULE8_TOP_SECRET_$timestamp"

Write-Step "Upload encrypted txt document"

$upload = Invoke-Upload `
    -Token $tokenA `
    -FileName "module8-$timestamp.txt" `
    -Content "$secret private rag content"

Assert-True `
    ($upload.StatusCode -eq 200 -and $upload.Json.code -eq 0) `
    "Upload failed. HTTP=$($upload.StatusCode), Raw=$($upload.Raw)"

$docId = [long]$upload.Json.data.id

Assert-True `
    ($docId -gt 0) `
    "Upload did not return document id. Raw=$($upload.Raw)"

Assert-True `
    (($upload.Json.data.status -eq "CHUNKED") -or ($upload.Json.data.status -eq "EMBEDDED")) `
    "Unexpected upload status. Raw=$($upload.Raw)"

Assert-NoSensitiveFields $upload.Raw

Write-Step "Embed document"

$embed = Invoke-Json `
    -Method "POST" `
    -Path "/api/documents/$docId/embed" `
    -Token $tokenA

Assert-True `
    ($embed.StatusCode -eq 200 -and $embed.Json.code -eq 0) `
    "Embed failed. HTTP=$($embed.StatusCode), Raw=$($embed.Raw)"

Assert-True `
    ($embed.Json.data.status -eq "EMBEDDED") `
    "Document did not reach EMBEDDED. Raw=$($embed.Raw)"

Assert-NoSensitiveFields $embed.Raw

Write-Step "Ask RAG question"

$ask = Invoke-Json `
    -Method "POST" `
    -Path "/api/chat/ask" `
    -Token $tokenA `
    -Body @{
        question   = "What private module eight content is available?"
        documentId = $docId
        topK       = 5
    }

Assert-True `
    ($ask.StatusCode -eq 200 -and $ask.Json.code -eq 0) `
    "Ask failed. HTTP=$($ask.StatusCode), Raw=$($ask.Raw)"

Assert-True `
    (-not [string]::IsNullOrWhiteSpace([string]$ask.Json.data.answer)) `
    "Ask answer is empty. Raw=$($ask.Raw)"

Assert-True `
    ($ask.Json.data.sources.Count -gt 0) `
    "Ask sources are empty. Raw=$($ask.Raw)"

Assert-NoSensitiveFields $ask.Raw

Write-Step "Check cross-user document isolation"

Assert-Status -Method "GET"  -Path "/api/documents/$docId"                  -Token $tokenB -ExpectedStatus 404
Assert-Status -Method "GET"  -Path "/api/documents/$docId/text"             -Token $tokenB -ExpectedStatus 404
Assert-Status -Method "GET"  -Path "/api/documents/$docId/chunks"           -Token $tokenB -ExpectedStatus 404
Assert-Status -Method "POST" -Path "/api/documents/$docId/embed"            -Token $tokenB -ExpectedStatus 404
Assert-Status -Method "GET"  -Path "/api/documents/$docId/embedding-status" -Token $tokenB -ExpectedStatus 404

Write-Step "Check cross-user semantic search isolation"

$searchB = Invoke-Json `
    -Method "POST" `
    -Path "/api/documents/search-chunks" `
    -Token $tokenB `
    -Body @{
        query = $secret
        topK  = 5
    }

Assert-True `
    ($searchB.StatusCode -eq 200 -and $searchB.Json.code -eq 0) `
    "User B search failed. HTTP=$($searchB.StatusCode), Raw=$($searchB.Raw)"

Assert-True `
    ($searchB.Json.data.Count -eq 0) `
    "User B search returned User A chunks. Raw=$($searchB.Raw)"

Assert-NoSensitiveFields $searchB.Raw

if ($ask.Json.data.conversationId) {
    $conversationId = [long]$ask.Json.data.conversationId

    Write-Step "Check conversation isolation"

    $messagesA = Invoke-Json `
        -Method "GET" `
        -Path "/api/conversations/$conversationId/messages" `
        -Token $tokenA

    Assert-True `
        ($messagesA.StatusCode -eq 200 -and $messagesA.Json.code -eq 0) `
        "User A messages read failed. HTTP=$($messagesA.StatusCode), Raw=$($messagesA.Raw)"

    Assert-NoSensitiveFields $messagesA.Raw

    Assert-Status `
        -Method "GET" `
        -Path "/api/conversations/$conversationId/messages" `
        -Token $tokenB `
        -ExpectedStatus 404
}

Write-Step "Check upload storage does not contain plaintext"

Assert-NoPlaintextInUploadStorage $secret

Write-Step "Delete document and verify cleanup"

$delete = Invoke-Json `
    -Method "DELETE" `
    -Path "/api/documents/$docId" `
    -Token $tokenA

Assert-True `
    ($delete.StatusCode -eq 200 -and $delete.Json.code -eq 0) `
    "Delete failed. HTTP=$($delete.StatusCode), Raw=$($delete.Raw)"

Assert-Status `
    -Method "GET" `
    -Path "/api/documents/$docId" `
    -Token $tokenA `
    -ExpectedStatus 404

Write-Host "MODULE 8 SMOKE TEST PASSED"