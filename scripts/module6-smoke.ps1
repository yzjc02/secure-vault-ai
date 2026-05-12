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
        $json = $Body | ConvertTo-Json -Depth 10 -Compress
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

    $tempFile = Join-Path ([IO.Path]::GetTempPath()) ("module6-smoke-" + [Guid]::NewGuid() + ".txt")
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
$userA = "module6a$suffix"
$userB = "module6b$suffix"

Write-Host "Register user A"
$tokenA = Register-And-Login -Name $userA

Write-Host "Register user B"
$tokenB = Register-And-Login -Name $userB

Write-Host "Upload txt as user A"
$upload = Upload-Txt -Token $tokenA -Text "Secure Vault AI module six semantic search text."

Assert-True ($upload.data.status -eq "CHUNKED") "Upload should end with CHUNKED status"
Assert-True ($null -ne $upload.data.id) "Upload returned document id"

$docId = [int64]$upload.data.id

Write-Host "Embed document as user A"
$embed = Invoke-Json -Method "POST" -Path "/api/documents/$docId/embed" -Token $tokenA

Assert-True ($embed.data.status -eq "EMBEDDED") "Embed should return EMBEDDED status"
Assert-True ([int]$embed.data.embeddedChunkCount -gt 0) "embeddedChunkCount should be greater than 0"

Write-Host "Get embedding status"
$status = Invoke-Json -Method "GET" -Path "/api/documents/$docId/embedding-status" -Token $tokenA

Assert-True ([int]$status.data.embeddedChunkCount -gt 0) "Status embeddedChunkCount should be greater than 0"

Write-Host "Search chunks as user A"
$search = Invoke-Json -Method "POST" -Path "/api/documents/search-chunks" -Token $tokenA -Body @{
    query = "semantic search"
    topK  = 5
}

$searchResults = @($search.data)

Assert-True ($searchResults.Count -gt 0) "Search results should not be empty"

$searchJson = $search | ConvertTo-Json -Depth 20 -Compress

Assert-True (-not $searchJson.Contains('"embedding":')) "Response must not contain embedding array"
Assert-True (-not $searchJson.Contains('"filePath":')) "Response must not contain filePath"
Assert-True (-not $searchJson.Contains('"userId":')) "Response must not contain userId"

Write-Host "Check user B isolation"

Invoke-ExpectStatus -Method "POST" -Path "/api/documents/$docId/embed" -Token $tokenB -ExpectedStatus 404
Invoke-ExpectStatus -Method "GET" -Path "/api/documents/$docId/embedding-status" -Token $tokenB -ExpectedStatus 404

$searchB = Invoke-Json -Method "POST" -Path "/api/documents/search-chunks" -Token $tokenB -Body @{
    query = "semantic search"
    topK  = 5
}

$searchBResults = @($searchB.data)

Assert-True ($searchBResults.Count -eq 0) "User B should not see user A chunks"

Write-Host "MODULE 6 SMOKE TEST PASSED"