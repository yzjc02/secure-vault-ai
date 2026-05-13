param(
    [string]$BaseUrl = "http://localhost:8080"
)

Add-Type -AssemblyName System.Net.Http

$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host "[MODULE10] $Message"
}

function Assert-True {
    param(
        [object]$Condition,
        [string]$Message
    )

    if (-not [bool]$Condition) {
        throw "FAIL: $Message"
    }
}

function Convert-ToJsonBody {
    param([hashtable]$Body)
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

function Assert-Success {
    param(
        [hashtable]$Response,
        [string]$Message
    )

    Assert-True `
        ($Response.StatusCode -eq 200 -and $Response.Json.code -eq 0) `
        "$Message HTTP=$($Response.StatusCode), Raw=$($Response.Raw)"
}

function Assert-NoSensitiveFields {
    param([string]$Raw)

    $blocked = @(
        "embedding",
        "embeddingJson",
        "embedding_json",
        "filePath",
        "userId",
        "fullPrompt",
        "storedFilename",
        "Bearer",
        "C:\",
        "/uploads/"
    )

    foreach ($item in $blocked) {
        Assert-True `
            (-not $Raw.Contains($item)) `
            "Response leaked sensitive content: $item. Raw=$Raw"
    }
}

function Assert-HasProperty {
    param(
        [object]$Object,
        [string]$Name,
        [string]$Raw
    )

    $hasProperty = $Object.PSObject.Properties.Name -contains $Name
    Assert-True $hasProperty "Missing property '$Name'. Raw=$Raw"
}

function Register-AndLogin {
    param([string]$Prefix)

    $suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $username = "$Prefix$suffix"
    $email = "$username@example.com"
    $password = "Password123!"

    Write-Step "Register $username"
    $register = Invoke-Json -Method "POST" -Path "/api/auth/register" -Body @{
        username = $username
        email    = $email
        password = $password
    }
    Assert-Success -Response $register -Message "Register failed for $username."

    Write-Step "Login $username"
    $login = Invoke-Json -Method "POST" -Path "/api/auth/login" -Body @{
        username = $username
        password = $password
    }
    Assert-Success -Response $login -Message "Login failed for $username."
    Assert-True `
        (-not [string]::IsNullOrWhiteSpace([string]$login.Json.data.token)) `
        "Login response did not include token. Raw=$($login.Raw)"

    return $login.Json.data.token
}

function Wait-BackendReady {
    $deadline = (Get-Date).AddSeconds(60)

    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-WebRequest -Method "GET" -Uri "$BaseUrl/api/me/audit-logs" | Out-Null
            return
        } catch [System.Net.WebException] {
            if ($null -ne $_.Exception.Response) {
                return
            }
            Start-Sleep -Seconds 2
        }
    }

    throw "FAIL: Backend did not become reachable within 60 seconds at $BaseUrl"
}

Write-Step "Wait for backend readiness"
Wait-BackendReady

Write-Step "Register and login users"
$tokenA = Register-AndLogin "module10a"
$tokenB = Register-AndLogin "module10b"
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

$knownPhrase = "This document verifies source tracing, chunk detail lookup, and user isolation."
$content = @"
Secure Vault AI module ten evidence trail test.
$knownPhrase
The answer must cite document id, chunk index, similarity score, and snippet.
"@

Write-Step "Upload txt as user A"
$upload = Invoke-Upload -Token $tokenA -FileName "module10-$timestamp.txt" -Content $content
Assert-Success -Response $upload -Message "Upload failed."
Assert-True `
    (@("CHUNKED", "EMBEDDED") -contains [string]$upload.Json.data.status) `
    "Upload did not return CHUNKED-compatible status. Raw=$($upload.Raw)"
Assert-NoSensitiveFields $upload.Raw

$docId = [long]$upload.Json.data.id
Assert-True ($docId -gt 0) "Upload did not return document id. Raw=$($upload.Raw)"

Write-Step "Embed document as user A"
$embed = Invoke-Json -Method "POST" -Path "/api/documents/$docId/embed" -Token $tokenA
Assert-Success -Response $embed -Message "Embed failed."
Assert-True ($embed.Json.data.status -eq "EMBEDDED") "Document did not reach EMBEDDED. Raw=$($embed.Raw)"
Assert-NoSensitiveFields $embed.Raw

Write-Step "Ask RAG question as user A"
$ask = Invoke-Json -Method "POST" -Path "/api/chat/ask" -Token $tokenA -Body @{
    question   = "What does module ten evidence trail verify?"
    documentId = $docId
    topK       = 5
}
Assert-Success -Response $ask -Message "RAG ask failed."
Assert-True (-not [string]::IsNullOrWhiteSpace([string]$ask.Json.data.answer)) "RAG answer is empty. Raw=$($ask.Raw)"
Assert-True ($ask.Json.data.sources.Count -gt 0) "RAG sources are empty. Raw=$($ask.Raw)"
Assert-NoSensitiveFields $ask.Raw

$source = $ask.Json.data.sources[0]
foreach ($name in @("documentId", "documentTitle", "originalFilename", "chunkIndex", "score", "snippet", "createdAt")) {
    Assert-HasProperty -Object $source -Name $name -Raw $ask.Raw
}
Assert-True ([long]$source.documentId -eq $docId) "RAG source documentId mismatch. Raw=$($ask.Raw)"
Assert-True (-not [string]::IsNullOrWhiteSpace([string]$source.documentTitle)) "RAG source documentTitle is empty. Raw=$($ask.Raw)"
Assert-True (-not [string]::IsNullOrWhiteSpace([string]$source.originalFilename)) "RAG source originalFilename is empty. Raw=$($ask.Raw)"
Assert-True ($null -ne $source.chunkIndex) "RAG source chunkIndex is null. Raw=$($ask.Raw)"
Assert-True ($null -ne $source.score) "RAG source score is null. Raw=$($ask.Raw)"
Assert-True (-not [string]::IsNullOrWhiteSpace([string]$source.snippet)) "RAG source snippet is empty. Raw=$($ask.Raw)"
Assert-True ($null -ne $source.createdAt) "RAG source createdAt is null. Raw=$($ask.Raw)"

$chunkIndex = [int]$source.chunkIndex

Write-Step "Get source chunk detail as user A"
$chunkDetail = Invoke-Json -Method "GET" -Path "/api/documents/$docId/chunks/$chunkIndex" -Token $tokenA
Assert-Success -Response $chunkDetail -Message "Chunk detail failed."
Assert-NoSensitiveFields $chunkDetail.Raw
Assert-True (-not [string]::IsNullOrWhiteSpace([string]$chunkDetail.Json.data.content)) "Chunk detail content is empty. Raw=$($chunkDetail.Raw)"
Assert-True ([string]$chunkDetail.Json.data.content -like "*$knownPhrase*") "Chunk detail content did not include known phrase. Raw=$($chunkDetail.Raw)"
foreach ($name in @("documentId", "documentTitle", "originalFilename", "chunkIndex", "content", "textLength", "createdAt")) {
    Assert-HasProperty -Object $chunkDetail.Json.data -Name $name -Raw $chunkDetail.Raw
}

Write-Step "User B attempts to access User A chunk detail"
$crossUser = Invoke-Json -Method "GET" -Path "/api/documents/$docId/chunks/$chunkIndex" -Token $tokenB
Assert-True `
    (@(404, 403) -contains [int]$crossUser.StatusCode) `
    "Cross-user chunk detail should return 404 or 403. HTTP=$($crossUser.StatusCode), Raw=$($crossUser.Raw)"
Assert-True `
    ([int]$crossUser.StatusCode -eq 404) `
    "Cross-user chunk detail should prefer 404. HTTP=$($crossUser.StatusCode), Raw=$($crossUser.Raw)"

Write-Host "MODULE 10 SMOKE TEST PASSED"
