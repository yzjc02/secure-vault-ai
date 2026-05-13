param(
    [string]$BaseUrl = "http://localhost:8080"
)

Add-Type -AssemblyName System.Net.Http

$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host "[MODULE9] $Message"
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

function Assert-Success {
    param(
        [hashtable]$Response,
        [string]$Message
    )

    Assert-True `
        ($Response.StatusCode -eq 200 -and $Response.Json.code -eq 0) `
        "$Message HTTP=$($Response.StatusCode), Raw=$($Response.Raw)"
}

function Assert-ExpectedStatus {
    param(
        [hashtable]$Response,
        [int]$ExpectedStatus,
        [string]$Message
    )

    Assert-True `
        ($Response.StatusCode -eq $ExpectedStatus) `
        "$Message Expected HTTP=$ExpectedStatus, actual HTTP=$($Response.StatusCode), Raw=$($Response.Raw)"
}

function Assert-AuditContains {
    param(
        [object]$AuditJson,
        [string]$Action
    )

    $found = $false
    foreach ($item in $AuditJson.data.items) {
        if ([string]$item.action -eq $Action) {
            $found = $true
            break
        }
    }

    Assert-True $found "Audit logs did not contain action $Action. Raw=$($AuditJson | ConvertTo-Json -Compress -Depth 10)"
}

function Assert-NoSensitiveFields {
    param([string]$Raw)

    $blocked = @(
        "userId",
        "password",
        "token",
        "Bearer",
        "filePath",
        "storedFilename",
        "fullPrompt",
        "embedding",
        "encryptionKey",
        "jdbc:",
        "C:\",
        "/uploads/"
    )

    foreach ($item in $blocked) {
        Assert-True `
            (-not $Raw.Contains($item)) `
            "Response leaked sensitive content: $item. Raw=$Raw"
    }
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

    throw "Backend did not become reachable within 60 seconds at $BaseUrl"
}

Write-Step "Wait for backend readiness"
Wait-BackendReady

Write-Step "Reject unauthenticated audit log access"

$unauthenticatedAudit = Invoke-Json -Method "GET" -Path "/api/me/audit-logs"
Assert-ExpectedStatus -Response $unauthenticatedAudit -ExpectedStatus 401 -Message "Unauthenticated audit log query should fail."

Write-Step "Register and login users"

$tokenA = Register-AndLogin "module9a"
$tokenB = Register-AndLogin "module9b"
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

Write-Step "Query User A initial audit logs"

$auditA1 = Invoke-Json -Method "GET" -Path "/api/me/audit-logs?size=100" -Token $tokenA
Assert-Success -Response $auditA1 -Message "User A audit log query failed."
Assert-AuditContains -AuditJson $auditA1.Json -Action "REGISTER_SUCCESS"
Assert-AuditContains -AuditJson $auditA1.Json -Action "LOGIN_SUCCESS"
Assert-NoSensitiveFields $auditA1.Raw

Write-Step "Upload User A txt document"

$upload = Invoke-Upload `
    -Token $tokenA `
    -FileName "module9-$timestamp.txt" `
    -Content "Secure Vault AI module nine validates audit logging, vector search, RAG, and access denied observability."

Assert-Success -Response $upload -Message "Upload failed."

$docId = [long]$upload.Json.data.id

Assert-True `
    ($docId -gt 0) `
    "Upload did not return document id. Raw=$($upload.Raw)"

Assert-NoSensitiveFields $upload.Raw

Write-Step "Embed User A document"

$embed = Invoke-Json -Method "POST" -Path "/api/documents/$docId/embed" -Token $tokenA
Assert-Success -Response $embed -Message "Embed failed."

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
        question   = "What does module nine validate?"
        documentId = $docId
        topK       = 5
    }

Assert-Success -Response $ask -Message "RAG ask failed."

Assert-True `
    (-not [string]::IsNullOrWhiteSpace([string]$ask.Json.data.answer)) `
    "RAG answer is empty. Raw=$($ask.Raw)"

Assert-NoSensitiveFields $ask.Raw

Write-Step "Query User A audit logs after upload, embed, and RAG"

$auditA2 = Invoke-Json -Method "GET" -Path "/api/me/audit-logs?size=100" -Token $tokenA
Assert-Success -Response $auditA2 -Message "User A audit log query failed."
Assert-AuditContains -AuditJson $auditA2.Json -Action "DOCUMENT_UPLOAD_SUCCESS"
Assert-AuditContains -AuditJson $auditA2.Json -Action "DOCUMENT_EMBED_SUCCESS"
Assert-AuditContains -AuditJson $auditA2.Json -Action "RAG_ASK_SUCCESS"
Assert-NoSensitiveFields $auditA2.Raw

Write-Step "User B attempts to access User A document"

$crossUserGet = Invoke-Json -Method "GET" -Path "/api/documents/$docId" -Token $tokenB
Assert-ExpectedStatus -Response $crossUserGet -ExpectedStatus 404 -Message "Cross-user document read should return 404."

Write-Step "Query User B audit logs for access denied"

$auditB1 = Invoke-Json -Method "GET" -Path "/api/me/audit-logs?size=100" -Token $tokenB
Assert-Success -Response $auditB1 -Message "User B audit log query failed."
Assert-AuditContains -AuditJson $auditB1.Json -Action "RESOURCE_ACCESS_DENIED"
Assert-NoSensitiveFields $auditB1.Raw

Write-Step "Delete User A document"

$delete = Invoke-Json -Method "DELETE" -Path "/api/documents/$docId" -Token $tokenA
Assert-Success -Response $delete -Message "Delete failed."
Assert-NoSensitiveFields $delete.Raw

Write-Step "Query User A audit logs after delete"

$auditA3 = Invoke-Json -Method "GET" -Path "/api/me/audit-logs?size=100" -Token $tokenA
Assert-Success -Response $auditA3 -Message "User A audit log query after delete failed."
Assert-AuditContains -AuditJson $auditA3.Json -Action "DOCUMENT_DELETE_SUCCESS"
Assert-NoSensitiveFields $auditA3.Raw

$auditLogId = [long]$auditA3.Json.data.items[0].id

Assert-True `
    ($auditLogId -gt 0) `
    "Could not resolve a User A audit log id. Raw=$($auditA3.Raw)"

Write-Step "User B cannot read User A audit log by id"

$auditLogReadByB = Invoke-Json -Method "GET" -Path "/api/me/audit-logs/$auditLogId" -Token $tokenB
Assert-ExpectedStatus -Response $auditLogReadByB -ExpectedStatus 404 -Message "User B audit log read should return 404."

Write-Host "MODULE 9 SMOKE TEST PASSED"
