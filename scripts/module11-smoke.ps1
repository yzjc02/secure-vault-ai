param(
    [string]$BaseUrl = "http://localhost:8080"
)

Add-Type -AssemblyName System.Net.Http

$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host "[MODULE11] $Message"
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
        "embeddingJson",
        "embedding_json",
        "`"embedding`"",
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
$tokenA = Register-AndLogin "module11a"
$tokenB = Register-AndLogin "module11b"
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

$knownPhrase = "manual reindex, chunk rebuild, embedding regeneration, and user isolation"
$content = @"
Secure Vault AI module eleven reindex test.
This document verifies $knownPhrase.
After reindex, RAG evidence trail must still cite document id, chunk index, similarity score, and snippet.
"@

Write-Step "Upload txt as user A"
$upload = Invoke-Upload -Token $tokenA -FileName "module11-$timestamp.txt" -Content $content
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

Write-Step "Ask RAG question as user A before reindex"
$askBefore = Invoke-Json -Method "POST" -Path "/api/chat/ask" -Token $tokenA -Body @{
    question   = "What does module eleven reindex verify?"
    documentId = $docId
    topK       = 5
}
Assert-Success -Response $askBefore -Message "RAG ask before reindex failed."
Assert-True (-not [string]::IsNullOrWhiteSpace([string]$askBefore.Json.data.answer)) "RAG answer before reindex is empty. Raw=$($askBefore.Raw)"
Assert-True ($askBefore.Json.data.sources.Count -gt 0) "RAG sources before reindex are empty. Raw=$($askBefore.Raw)"
Assert-NoSensitiveFields $askBefore.Raw

$sourceBefore = $askBefore.Json.data.sources[0]
Assert-True ([long]$sourceBefore.documentId -eq $docId) "Before-reindex source documentId mismatch. Raw=$($askBefore.Raw)"
Assert-True ($null -ne $sourceBefore.chunkIndex) "Before-reindex source chunkIndex is null. Raw=$($askBefore.Raw)"
$beforeChunkIndex = [int]$sourceBefore.chunkIndex

Write-Step "Reindex document as user A"
$reindex = Invoke-Json -Method "POST" -Path "/api/documents/$docId/reindex" -Token $tokenA
Assert-Success -Response $reindex -Message "Reindex failed."
Assert-NoSensitiveFields $reindex.Raw
foreach ($name in @("documentId", "title", "status", "chunkCount", "embeddedChunkCount", "reindexedAt")) {
    Assert-HasProperty -Object $reindex.Json.data -Name $name -Raw $reindex.Raw
}
Assert-True ([long]$reindex.Json.data.documentId -eq $docId) "Reindex documentId mismatch. Raw=$($reindex.Raw)"
Assert-True ($reindex.Json.data.status -eq "EMBEDDED") "Reindex did not return EMBEDDED. Raw=$($reindex.Raw)"
Assert-True ([int]$reindex.Json.data.chunkCount -gt 0) "Reindex chunkCount should be > 0. Raw=$($reindex.Raw)"
Assert-True ([int]$reindex.Json.data.embeddedChunkCount -gt 0) "Reindex embeddedChunkCount should be > 0. Raw=$($reindex.Raw)"
Assert-True ($null -ne $reindex.Json.data.reindexedAt) "Reindex reindexedAt is null. Raw=$($reindex.Raw)"

Write-Step "Ask RAG question as user A after reindex"
$askAfter = Invoke-Json -Method "POST" -Path "/api/chat/ask" -Token $tokenA -Body @{
    question   = "What should the evidence trail cite after reindex?"
    documentId = $docId
    topK       = 5
}
Assert-Success -Response $askAfter -Message "RAG ask after reindex failed."
Assert-True (-not [string]::IsNullOrWhiteSpace([string]$askAfter.Json.data.answer)) "RAG answer after reindex is empty. Raw=$($askAfter.Raw)"
Assert-True ($askAfter.Json.data.sources.Count -gt 0) "RAG sources after reindex are empty. Raw=$($askAfter.Raw)"
Assert-NoSensitiveFields $askAfter.Raw

$sourceAfter = $askAfter.Json.data.sources[0]
foreach ($name in @("documentId", "documentTitle", "originalFilename", "chunkIndex", "score", "snippet", "createdAt")) {
    Assert-HasProperty -Object $sourceAfter -Name $name -Raw $askAfter.Raw
}
Assert-True ([long]$sourceAfter.documentId -eq $docId) "After-reindex source documentId mismatch. Raw=$($askAfter.Raw)"
Assert-True (-not [string]::IsNullOrWhiteSpace([string]$sourceAfter.documentTitle)) "After-reindex documentTitle is empty. Raw=$($askAfter.Raw)"
Assert-True (-not [string]::IsNullOrWhiteSpace([string]$sourceAfter.originalFilename)) "After-reindex originalFilename is empty. Raw=$($askAfter.Raw)"
Assert-True ($null -ne $sourceAfter.chunkIndex) "After-reindex chunkIndex is null. Raw=$($askAfter.Raw)"
Assert-True ($null -ne $sourceAfter.score) "After-reindex score is null. Raw=$($askAfter.Raw)"
Assert-True (-not [string]::IsNullOrWhiteSpace([string]$sourceAfter.snippet)) "After-reindex snippet is empty. Raw=$($askAfter.Raw)"
Assert-True ($null -ne $sourceAfter.createdAt) "After-reindex createdAt is null. Raw=$($askAfter.Raw)"

$chunkIndex = [int]$sourceAfter.chunkIndex

Write-Step "Get source chunk detail as user A"
$chunkDetail = Invoke-Json -Method "GET" -Path "/api/documents/$docId/chunks/$chunkIndex" -Token $tokenA
Assert-Success -Response $chunkDetail -Message "Chunk detail after reindex failed."
Assert-NoSensitiveFields $chunkDetail.Raw
Assert-True (-not [string]::IsNullOrWhiteSpace([string]$chunkDetail.Json.data.content)) "Chunk detail content is empty. Raw=$($chunkDetail.Raw)"
Assert-True ([string]$chunkDetail.Json.data.content -like "*$knownPhrase*") "Chunk detail content did not include known phrase. Raw=$($chunkDetail.Raw)"
foreach ($name in @("documentId", "documentTitle", "originalFilename", "chunkIndex", "content", "textLength", "createdAt")) {
    Assert-HasProperty -Object $chunkDetail.Json.data -Name $name -Raw $chunkDetail.Raw
}

Write-Step "User B attempts to reindex User A document"
$crossUserReindex = Invoke-Json -Method "POST" -Path "/api/documents/$docId/reindex" -Token $tokenB
Assert-True `
    (@(404, 403) -contains [int]$crossUserReindex.StatusCode) `
    "Cross-user reindex should return 404 or 403. HTTP=$($crossUserReindex.StatusCode), Raw=$($crossUserReindex.Raw)"
Assert-True `
    ([int]$crossUserReindex.StatusCode -eq 404) `
    "Cross-user reindex should prefer 404. HTTP=$($crossUserReindex.StatusCode), Raw=$($crossUserReindex.Raw)"
Assert-NoSensitiveFields $crossUserReindex.Raw

Write-Step "User B attempts to access User A chunk detail"
$crossUserChunk = Invoke-Json -Method "GET" -Path "/api/documents/$docId/chunks/$chunkIndex" -Token $tokenB
Assert-True `
    (@(404, 403) -contains [int]$crossUserChunk.StatusCode) `
    "Cross-user chunk detail should return 404 or 403. HTTP=$($crossUserChunk.StatusCode), Raw=$($crossUserChunk.Raw)"
Assert-True `
    ([int]$crossUserChunk.StatusCode -eq 404) `
    "Cross-user chunk detail should prefer 404. HTTP=$($crossUserChunk.StatusCode), Raw=$($crossUserChunk.Raw)"
Assert-NoSensitiveFields $crossUserChunk.Raw

Write-Step "Recorded before-reindex source documentId=$docId chunkIndex=$beforeChunkIndex"
Write-Host "MODULE 11 SMOKE TEST PASSED"
