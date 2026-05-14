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
    "backend/src/main/java/com/jiacheng/securevault/document/controller/DocumentController.java",
    "backend/src/main/java/com/jiacheng/securevault/document/dto/DocumentReindexResponse.java",
    "backend/src/main/java/com/jiacheng/securevault/document/entity/Document.java",
    "backend/src/main/java/com/jiacheng/securevault/document/repository/DocumentChunkRepository.java",
    "backend/src/main/java/com/jiacheng/securevault/document/repository/DocumentRepository.java",
    "backend/src/main/java/com/jiacheng/securevault/document/service/DocumentReindexService.java",
    "backend/src/main/java/com/jiacheng/securevault/audit/enums/AuditAction.java",
    "backend/src/test/java/com/jiacheng/securevault/document/DocumentReindexControllerTest.java",
    "backend/src/test/java/com/jiacheng/securevault/document/DocumentReindexServiceTest.java",
    "scripts/module11-smoke.ps1",
    "scripts/module11-verify.ps1",
    "docs/reindex-design.md",
    "README.md"
)

Write-Step "Check required Module 11 files"

foreach ($file in $requiredFiles) {
    Assert-True (Test-Path -LiteralPath (Join-Path $root $file) -PathType Leaf) "Missing required file: $file"
}

$controller = Read-Utf8 (Join-Path $root "backend/src/main/java/com/jiacheng/securevault/document/controller/DocumentController.java")
$response = Read-Utf8 (Join-Path $root "backend/src/main/java/com/jiacheng/securevault/document/dto/DocumentReindexResponse.java")
$document = Read-Utf8 (Join-Path $root "backend/src/main/java/com/jiacheng/securevault/document/entity/Document.java")
$chunkRepository = Read-Utf8 (Join-Path $root "backend/src/main/java/com/jiacheng/securevault/document/repository/DocumentChunkRepository.java")
$documentRepository = Read-Utf8 (Join-Path $root "backend/src/main/java/com/jiacheng/securevault/document/repository/DocumentRepository.java")
$service = Read-Utf8 (Join-Path $root "backend/src/main/java/com/jiacheng/securevault/document/service/DocumentReindexService.java")
$auditAction = Read-Utf8 (Join-Path $root "backend/src/main/java/com/jiacheng/securevault/audit/enums/AuditAction.java")
$controllerTest = Read-Utf8 (Join-Path $root "backend/src/test/java/com/jiacheng/securevault/document/DocumentReindexControllerTest.java")
$serviceTest = Read-Utf8 (Join-Path $root "backend/src/test/java/com/jiacheng/securevault/document/DocumentReindexServiceTest.java")
$smoke = Read-Utf8 (Join-Path $root "scripts/module11-smoke.ps1")
$readme = Read-Utf8 (Join-Path $root "README.md")
$design = Read-Utf8 (Join-Path $root "docs/reindex-design.md")

Write-Step "Check API and DTO"

foreach ($term in @(
    '@PostMapping("/{documentId}/reindex")',
    "DocumentReindexResponse",
    "documentId",
    "title",
    "status",
    "chunkCount",
    "embeddedChunkCount",
    "reindexedAt"
)) {
    Assert-Contains -Content ($controller + "`n" + $response) -Term $term -Path "DocumentController/DocumentReindexResponse"
}

foreach ($term in @("STATUS_REINDEXING", "REINDEXING", "STATUS_REINDEX_FAILED", "REINDEX_FAILED")) {
    Assert-Contains -Content $document -Term $term -Path "Document.java"
}

Write-Step "Check safe bulk delete"

foreach ($term in @(
    "@Modifying",
    "@Query",
    "delete from DocumentChunk",
    "chunk.userId = :userId",
    "chunk.documentId = :documentId",
    "deleteByUserIdAndDocumentIdDirectly"
)) {
    Assert-Contains -Content $chunkRepository -Term $term -Path "DocumentChunkRepository.java"
}

Assert-True (-not $chunkRepository.Contains("int deleteByUserIdAndDocumentId(")) "Repository must not expose confusing derived delete method name"
Assert-Contains -Content $documentRepository -Term "findByIdAndUserIdForUpdate" -Path "DocumentRepository.java"
Assert-Contains -Content $documentRepository -Term "PESSIMISTIC_WRITE" -Path "DocumentRepository.java"

Write-Step "Check service flow"

foreach ($term in @(
    "prepareReindex",
    "executeReindex",
    "markReindexFailed",
    "deleteByUserIdAndDocumentIdDirectly",
    "textChunkingService.split",
    "embeddingClient.embed",
    "chunkEmbeddingStore.saveEmbedding",
    "Document.STATUS_REINDEXING",
    "Document.STATUS_EMBEDDED",
    "Document.STATUS_REINDEX_FAILED",
    "DOCUMENT_NOT_PARSED",
    "DOCUMENT_REINDEXING",
    "DOCUMENT_REINDEX"
)) {
    Assert-Contains -Content $service -Term $term -Path "DocumentReindexService.java"
}

Assert-Contains -Content $auditAction -Term "DOCUMENT_REINDEX" -Path "AuditAction.java"
Assert-True (-not $service.Contains("getFilePath()")) "Reindex service must not use filePath"
Assert-True (-not $service.Contains("getStoredFilename()")) "Reindex service must not re-read uploaded file"

Write-Step "Check tests"

foreach ($term in @(
    "shouldReindexDocumentAndKeepRagEvidenceTrailWorking",
    "shouldReturn404ForCrossUserReindexWithoutChangingOwnerDocument",
    "shouldRequireAuthenticationForReindex",
    "shouldReturn400WhenExtractedTextIsEmpty",
    "shouldKeepChunkCountStableAcrossRepeatedReindexCalls",
    "shouldRejectReindexWhenDocumentIsAlreadyReindexing",
    "assertNoSensitiveFields",
    "DOCUMENT_REINDEX"
)) {
    Assert-Contains -Content $controllerTest -Term $term -Path "DocumentReindexControllerTest.java"
}

foreach ($term in @(
    "shouldMarkDocumentReindexFailedAndAuditWhenEmbeddingFails",
    "STATUS_REINDEX_FAILED",
    "deleteByUserIdAndDocumentIdDirectly",
    "newStatus=REINDEX_FAILED"
)) {
    Assert-Contains -Content $serviceTest -Term $term -Path "DocumentReindexServiceTest.java"
}

Write-Step "Check smoke script"

foreach ($term in @(
    'param(',
    '$BaseUrl = "http://localhost:8080"',
    "Register-AndLogin",
    "email",
    "/api/documents/upload",
    '/api/documents/$docId/embed',
    "/api/chat/ask",
    '/api/documents/$docId/reindex',
    "documentId",
    "documentTitle",
    "originalFilename",
    "chunkIndex",
    "score",
    "snippet",
    "createdAt",
    "MODULE 11 SMOKE TEST PASSED"
)) {
    Assert-Contains -Content $smoke -Term $term -Path "module11-smoke.ps1"
}

foreach ($term in @("embeddingJson", "embedding_json", "filePath", "userId", "fullPrompt", "storedFilename")) {
    Assert-Contains -Content $smoke -Term $term -Path "module11-smoke.ps1"
}

Write-Step "Check README and design doc"

foreach ($term in @(
    "Module 11: Document Reindex",
    "POST /api/documents/{documentId}/reindex",
    'existing `extractedText`',
    "safe bulk delete",
    '`REINDEXING`, `EMBEDDED`, or `REINDEX_FAILED`',
    "users can only reindex their own documents",
    "RAG Evidence Trail"
)) {
    Assert-Contains -Content $readme -Term $term -Path "README.md"
}

foreach ($term in @(
    "## 1.",
    "## 2.",
    "## 3.",
    "## 4.",
    "## 5.",
    "bulk delete",
    "@Modifying + @Query",
    "Bad value for type long",
    "## 7.",
    "## 8.",
    "audit log",
    "## 10.",
    "## 11.",
    "## 12."
)) {
    Assert-Contains -Content $design -Term $term -Path "docs/reindex-design.md"
}

Write-Step "Run safety checks"

$combined = $controller + "`n" + $response + "`n" + $document + "`n" + $chunkRepository + "`n" +
        $documentRepository + "`n" + $service + "`n" + $controllerTest + "`n" + $serviceTest + "`n" +
        $smoke + "`n" + $readme + "`n" + $design

$unfinishedWords = @("TO" + "DO", "TB" + "D", "FIX" + "ME")
$escapedUnfinishedWords = $unfinishedWords | ForEach-Object { [regex]::Escape($_) }
$unfinishedPattern = '(?i)\b(' + ($escapedUnfinishedWords -join "|") + ')\b'
Assert-True (-not ($combined -match $unfinishedPattern)) "Module 11 files contain unfinished placeholder words"
Assert-True (-not ($combined -match 'Authorization:\s*Bearer\s+eyJ[A-Za-z0-9_\-]+')) "Module 11 files contain a JWT-looking Authorization header"
Assert-True (-not ($combined -match '\bBearer\s+eyJ[A-Za-z0-9_\-]+')) "Module 11 files contain a JWT-looking bearer token sample"
Assert-True (-not ($combined -match '(?i)C:\\Users\\')) "Module 11 files contain a real-looking Windows user directory"
Assert-True (-not $response.Contains("userId")) "DocumentReindexResponse must not expose userId"
Assert-True (-not $response.Contains("filePath")) "DocumentReindexResponse must not expose filePath"
Assert-True (-not $response.Contains("storedFilename")) "DocumentReindexResponse must not expose storedFilename"
Assert-True (-not $response.Contains("embeddingJson")) "DocumentReindexResponse must not expose embeddingJson"
Assert-True (-not $response.Contains("fullPrompt")) "DocumentReindexResponse must not expose fullPrompt"

Write-Host "MODULE 11 REINDEX VERIFY PASSED"
