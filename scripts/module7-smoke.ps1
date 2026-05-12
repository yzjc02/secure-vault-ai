param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
Add-Type -AssemblyName System.Net.Http

function Assert-True {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Condition,

        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    if (-not ([bool]$Condition)) {
        throw "ASSERT FAILED: $Message"
    }

    Write-Host "PASS: $Message"
}

function Get-Prop {
    param(
        [object]$Object,
        [string[]]$Names
    )

    if ($null -eq $Object) {
        return $null
    }

    foreach ($name in $Names) {
        if ($null -ne $Object.PSObject.Properties[$name]) {
            return $Object.$name
        }
    }

    return $null
}

function Get-ItemsFromApiData {
    param(
        [object]$Data
    )

    if ($null -eq $Data) {
        return @()
    }

    if ($Data -is [System.Array]) {
        return @($Data)
    }

    $possibleArray = Get-Prop $Data @(
        "items",
        "content",
        "conversations",
        "messages",
        "data",
        "list",
        "records"
    )

    if ($null -ne $possibleArray) {
        if ($possibleArray -is [System.Array]) {
            return @($possibleArray)
        }

        return @($possibleArray)
    }

    return @($Data)
}

function Invoke-Json {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = $null
    )

    $headers = @{}

    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers.Authorization = "Bearer $Token"
    }

    $params = @{
        Method      = $Method
        Uri         = "$BaseUrl$Path"
        Headers     = $headers
        ContentType = "application/json; charset=utf-8"
    }

    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 20 -Compress
        $params.Body = [Text.Encoding]::UTF8.GetBytes($json)
    }

    return Invoke-RestMethod @params
}

function Invoke-ExpectStatus {
    param(
        [string]$Method,
        [string]$Path,
        [int]$ExpectedStatus,
        [object]$Body = $null,
        [string]$Token = $null
    )

    try {
        Invoke-Json -Method $Method -Path $Path -Body $Body -Token $Token | Out-Null
        throw "Expected HTTP $ExpectedStatus for $Method $Path, but request succeeded"
    } catch {
        if ($null -eq $_.Exception.Response) {
            throw $_
        }

        $status = [int]$_.Exception.Response.StatusCode.value__
        Assert-True ($status -eq $ExpectedStatus) "Expected HTTP $ExpectedStatus for $Method $Path, got $status"
    }
}

function Register-And-Login {
    param(
        [string]$Name
    )

    Invoke-Json -Method "POST" -Path "/api/auth/register" -Body @{
        username = $Name
        email    = "$Name@example.com"
        password = "Password123"
    } | Out-Null

    $login = Invoke-Json -Method "POST" -Path "/api/auth/login" -Body @{
        username = $Name
        password = "Password123"
    }

    Assert-True (-not [string]::IsNullOrWhiteSpace([string]$login.data.token)) "Login returned token for $Name"

    return [string]$login.data.token
}

function Upload-Txt {
    param(
        [string]$Token,
        [string]$Text
    )

    $tempFile = Join-Path ([IO.Path]::GetTempPath()) ("module7-smoke-" + [Guid]::NewGuid() + ".txt")
    [IO.File]::WriteAllText($tempFile, $Text, [Text.UTF8Encoding]::new($false))

    $client = $null
    $content = $null
    $fileStream = $null
    $fileContent = $null

    try {
        $client = [System.Net.Http.HttpClient]::new()

        $client.DefaultRequestHeaders.Authorization =
            [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)

        $content = [System.Net.Http.MultipartFormDataContent]::new()

        $fileStream = [System.IO.File]::OpenRead($tempFile)
        $fileContent = [System.Net.Http.StreamContent]::new($fileStream)

        $fileContent.Headers.ContentType =
            [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("text/plain")

        $fileName = [System.IO.Path]::GetFileName($tempFile)

        $content.Add($fileContent, "file", $fileName)

        $response = $client.PostAsync("$BaseUrl/api/documents/upload", $content).Result
        $responseText = $response.Content.ReadAsStringAsync().Result

        if (-not $response.IsSuccessStatusCode) {
            throw "Upload failed. HTTP $([int]$response.StatusCode). Response: $responseText"
        }

        return $responseText | ConvertFrom-Json
    } finally {
        if ($fileContent) {
            $fileContent.Dispose()
        }

        if ($fileStream) {
            $fileStream.Dispose()
        }

        if ($content) {
            $content.Dispose()
        }

        if ($client) {
            $client.Dispose()
        }

        Remove-Item -LiteralPath $tempFile -Force -ErrorAction SilentlyContinue
    }
}

$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$userA = "module7a$suffix"
$userB = "module7b$suffix"

Write-Host "Register user A"
$tokenA = Register-And-Login -Name $userA

Write-Host "Register user B"
$tokenB = Register-And-Login -Name $userB

Write-Host "Upload txt as user A"
$upload = Upload-Txt -Token $tokenA -Text "Secure Vault AI module seven validates RAG question answering with sources and strict user isolation."

Assert-True ($upload.data.status -eq "CHUNKED") "Upload should end with CHUNKED status"
Assert-True ($null -ne $upload.data.id) "Upload returned document id"

$docId = [int64]$upload.data.id

Write-Host "Embed document as user A"
$embed = Invoke-Json -Method "POST" -Path "/api/documents/$docId/embed" -Token $tokenA

Assert-True ($embed.data.status -eq "EMBEDDED") "Embed should return EMBEDDED status"
Assert-True ([int]$embed.data.embeddedChunkCount -gt 0) "embeddedChunkCount should be greater than 0"

Write-Host "Ask RAG question as user A"
$ask = Invoke-Json -Method "POST" -Path "/api/chat/ask" -Token $tokenA -Body @{
    question   = "What does module seven validate?"
    topK       = 5
    documentId = $docId
}

Assert-True (-not [string]::IsNullOrWhiteSpace([string]$ask.data.answer)) "Ask returned non-empty answer"

$sources = @($ask.data.sources)
Assert-True ($sources.Count -gt 0) "Ask returned sources"
Assert-True ([int64]$sources[0].documentId -eq $docId) "First source belongs to uploaded document"

$askJson = $ask | ConvertTo-Json -Depth 30 -Compress
Assert-True (-not $askJson.Contains('"embedding":')) "Ask response must not contain embedding array"
Assert-True (-not $askJson.Contains('"filePath":')) "Ask response must not contain filePath"
Assert-True (-not $askJson.Contains('"userId":')) "Ask response must not contain userId"
Assert-True (-not $askJson.Contains('"fullPrompt":')) "Ask response must not contain fullPrompt"

$conversationId = [int64]$ask.data.conversationId
Assert-True ($conversationId -gt 0) "Ask returned conversationId"

Write-Host "List conversations as user A"
$conversations = Invoke-Json -Method "GET" -Path "/api/conversations" -Token $tokenA
$conversationRows = @(Get-ItemsFromApiData $conversations.data)

$currentConversation = $conversationRows | Where-Object {
    $rowConversationId = Get-Prop $_ @("conversationId", "id")
    [string]$rowConversationId -eq [string]$conversationId
} | Select-Object -First 1

Assert-True ($null -ne $currentConversation) "Conversation list contains current conversation"

Write-Host "Get conversation messages as user A"
$messages = Invoke-Json -Method "GET" -Path "/api/conversations/$conversationId/messages" -Token $tokenA
$messageRows = @(Get-ItemsFromApiData $messages.data)

Assert-True ($messageRows.Count -ge 2) "Conversation has at least USER and ASSISTANT messages"

$userMessage = $messageRows | Where-Object {
    $role = Get-Prop $_ @("role", "chatRole")
    [string]$role -eq "USER"
} | Select-Object -First 1

$assistantMessage = $messageRows | Where-Object {
    $role = Get-Prop $_ @("role", "chatRole")
    [string]$role -eq "ASSISTANT"
} | Select-Object -First 1

Assert-True ($null -ne $userMessage) "Conversation contains USER message"
Assert-True ($null -ne $assistantMessage) "Conversation contains ASSISTANT message"

Write-Host "Check user B isolation"
Invoke-ExpectStatus -Method "POST" -Path "/api/chat/ask" -Token $tokenB -ExpectedStatus 404 -Body @{
    question   = "What does module seven validate?"
    topK       = 5
    documentId = $docId
}

Invoke-ExpectStatus -Method "GET" -Path "/api/conversations/$conversationId/messages" -Token $tokenB -ExpectedStatus 404

$askB = Invoke-Json -Method "POST" -Path "/api/chat/ask" -Token $tokenB -Body @{
    question = "What does module seven validate?"
    topK     = 5
}

$sourcesB = @($askB.data.sources)
Assert-True ($sourcesB.Count -eq 0) "User B should not see user A sources"

Write-Host "MODULE 7 SMOKE TEST PASSED"