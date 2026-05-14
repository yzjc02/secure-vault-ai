# Secure Vault AI API Guide

本文档按当前 `Controller` 和 DTO 整理接口，不包含源码中不存在的接口。

## 1. 统一响应格式

成功响应使用 `ApiResponse`：

```json
{
  "code": 0,
  "message": "OK",
  "data": {}
}
```

错误响应也使用相同 envelope：

```json
{
  "code": 400,
  "message": "请求参数不完整",
  "data": null
}
```

常见 HTTP 状态码：

| HTTP | 场景 |
| --- | --- |
| `400` | 参数校验失败、JSON 格式错误、文件格式或大小不合法、文档未解析、embedding 参数不合法 |
| `401` | 未登录、JWT 缺失、JWT 无效或过期 |
| `403` | Spring Security 拒绝访问时返回 |
| `404` | 资源不存在或不属于当前用户 |
| `500` | 服务器内部错误、文件加密/解密或存储失败等 |

## 2. 鉴权说明

除 `/api/auth/register` 和 `/api/auth/login` 外，其他接口需要请求头：

```text
Authorization: Bearer <token>
```

示例中的 `<token>`、`<documentId>`、`<auditLogId>` 都是占位符，不要替换成真实密钥或在文档中保存真实 JWT。

## 3. Auth

### 注册

- Method: `POST`
- Path: `/api/auth/register`
- Auth required: No
- Content-Type: `application/json; charset=utf-8`

Request body:

```json
{
  "username": "demo_user",
  "email": "demo_user@example.com",
  "password": "Password123!"
}
```

Response example:

```json
{
  "code": 0,
  "message": "OK",
  "data": null
}
```

Error cases:

- `400`：用户名为空、邮箱为空、邮箱格式不正确、密码长度不合法、用户名或邮箱已存在。

Security notes:

- 密码由后端 BCrypt 后保存。
- 注册接口必须带 `email` 字段。

PowerShell:

```powershell
$baseUrl = "http://localhost:8080"
$registerBody = @{
  username = "demo_user"
  email = "demo_user@example.com"
  password = "Password123!"
} | ConvertTo-Json -Compress

Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/auth/register" `
  -ContentType "application/json; charset=utf-8" `
  -Body $registerBody
```

### 登录

- Method: `POST`
- Path: `/api/auth/login`
- Auth required: No
- Content-Type: `application/json; charset=utf-8`

Request body:

```json
{
  "username": "demo_user",
  "password": "Password123!"
}
```

Response example:

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "token": "<token>",
    "tokenType": "Bearer"
  }
}
```

Error cases:

- `400`：用户名或密码为空。
- `401`：用户名或密码错误。

PowerShell:

```powershell
$loginBody = @{
  username = "demo_user"
  password = "Password123!"
} | ConvertTo-Json -Compress

$loginResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/auth/login" `
  -ContentType "application/json; charset=utf-8" `
  -Body $loginBody

$token = $loginResponse.data.token
```

## 4. Documents

### 创建文档记录

- Method: `POST`
- Path: `/api/documents`
- Auth required: Yes
- Content-Type: `application/json; charset=utf-8`

Request body:

```json
{
  "title": "Spring Security 学习笔记",
  "description": "记录 JWT 鉴权流程"
}
```

Response data fields include:

```json
{
  "id": 1,
  "title": "Spring Security 学习笔记",
  "description": "记录 JWT 鉴权流程",
  "status": "CREATED",
  "chunkCount": 0,
  "embeddedChunkCount": 0,
  "createdAt": "2026-05-13T10:00:00",
  "updatedAt": "2026-05-13T10:00:00"
}
```

Error cases:

- `400`：标题为空、标题超过 120、描述超过 1000。
- `401`：未携带有效 JWT。

### 上传文档

- Method: `POST`
- Path: `/api/documents/upload`
- Auth required: Yes
- Content-Type: `multipart/form-data`
- Form fields:
  - `file`: required
  - `title`: optional

真实行为：上传成功后会自动解析并自动分块。可解析文本时返回状态通常为 `CHUNKED`；解析失败时返回 `FAILED`，并保留安全错误摘要。

Response example:

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "id": 1,
    "title": "demo.txt",
    "status": "CHUNKED",
    "originalFilename": "demo.txt",
    "fileType": "txt",
    "fileSize": 128,
    "contentType": "text/plain",
    "textLength": 120,
    "chunkCount": 1,
    "embeddedChunkCount": 0
  }
}
```

PowerShell:

```powershell
$form = @{
  file = Get-Item ".\demo.txt"
  title = "演示文档"
}

Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/documents/upload" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Form $form
```

Error cases:

- `400`：缺少文件、空文件、文件过大、不支持的扩展名、multipart 格式错误。
- `401`：未登录。

Security notes:

- 响应不会返回 `storedFilename` 或服务器本地 `filePath`。
- 文件落盘前会经过 AES-GCM 加密。

### 获取文档列表

- Method: `GET`
- Path: `/api/documents`
- Auth required: Yes

Response example:

```json
{
  "code": 0,
  "message": "OK",
  "data": [
    {
      "id": 1,
      "title": "demo.txt",
      "status": "CHUNKED",
      "originalFilename": "demo.txt",
      "chunkCount": 1,
      "embeddedChunkCount": 0
    }
  ]
}
```

Security notes:

- 只返回当前用户文档。
- 列表不返回完整 `extractedText`。

### 获取文档详情

- Method: `GET`
- Path: `/api/documents/{id}`
- Auth required: Yes

Response data may include `extractedTextPreview`，但不会返回完整解析文本。

Error cases:

- `404`：文档不存在或属于其他用户。

### 更新文档记录

- Method: `PUT`
- Path: `/api/documents/{id}`
- Auth required: Yes
- Content-Type: `application/json; charset=utf-8`

Request body:

```json
{
  "title": "新的标题",
  "description": "新的描述"
}
```

Error cases:

- `400`：标题为空或长度超限。
- `404`：文档不存在或属于其他用户。

### 获取文档完整文本

- Method: `GET`
- Path: `/api/documents/{id}/text`
- Auth required: Yes

Response example:

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "id": 1,
    "title": "demo.txt",
    "status": "CHUNKED",
    "originalFilename": "demo.txt",
    "fileType": "txt",
    "textLength": 120,
    "parsedAt": "2026-05-13T10:01:00",
    "extractedText": "完整解析文本..."
  }
}
```

Error cases:

- `400`：文档尚未成功解析。
- `404`：文档不存在或属于其他用户。

### 删除文档

- Method: `DELETE`
- Path: `/api/documents/{id}`
- Auth required: Yes

Response:

```json
{
  "code": 0,
  "message": "OK",
  "data": null
}
```

Security notes:

- 删除前校验当前用户所有权。
- 删除会清理本地加密文件、当前文档 chunks 和 embedding 数据。

## 5. Parsing / Chunking

### 手动重新解析

- Method: `POST`
- Path: `/api/documents/{id}/parse`
- Auth required: Yes

说明：

- 仅适用于有上传文件的文档。
- 重新解析会清理旧 chunks，并在解析成功后重新分块。

Error cases:

- `400`：文档没有上传文件。
- `404`：文档不存在或属于其他用户。

### 手动重新分块

- Method: `POST`
- Path: `/api/documents/{id}/chunk`
- Auth required: Yes

说明：

- 基于 `documents.extracted_text` 分块。
- 文档正在解析或没有成功解析文本时返回 `400`。

### 获取 chunks

- Method: `GET`
- Path: `/api/documents/{id}/chunks`
- Auth required: Yes

Response example:

```json
{
  "code": 0,
  "message": "OK",
  "data": [
    {
      "id": 10,
      "documentId": 1,
      "chunkIndex": 0,
      "content": "chunk text",
      "contentLength": 10,
      "tokenCount": 3,
      "contentHash": "hash",
      "startOffset": 0,
      "endOffset": 10,
      "createdAt": "2026-05-13T10:02:00"
    }
  ]
}
```

Security notes:

- 只返回当前用户该文档的 chunks。
- chunk 响应不包含 `userId`、本地路径或 embedding 数组。

## 6. Embedding / Search

### 执行 embedding

- Method: `POST`
- Path: `/api/documents/{id}/embed`
- Auth required: Yes

说明：

- 对当前用户该文档的 chunks 生成 embedding。
- 默认 `EMBEDDING_PROVIDER=deterministic`，可切换为 Ollama。

Response data 重点字段：

```json
{
  "id": 1,
  "status": "EMBEDDED",
  "chunkCount": 1,
  "embeddedChunkCount": 1,
  "embeddedAt": "2026-05-13T10:03:00"
}
```

Error cases:

- `400`：文档没有 chunks、embedding 失败、维度不匹配。
- `404`：文档不存在或属于其他用户。

### 手动重新索引

- Method: `POST`
- Path: `/api/documents/{documentId}/reindex`
- Auth required: Yes

说明：

- 基于当前文档已有 `extractedText` 重新分块并重新生成 embeddings。
- 不重新上传文件，不重新解析原始文件，不删除 Document 本体。
- 旧 chunks 使用 `@Modifying + @Query` bulk delete 删除，避免加载较大的 `embedding_json`。
- 成功后文档状态为 `EMBEDDED`；处理中状态为 `REINDEXING`；失败状态为 `REINDEX_FAILED`。

Response example:

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "documentId": 31,
    "title": "模块十一测试文档",
    "status": "EMBEDDED",
    "chunkCount": 8,
    "embeddedChunkCount": 8,
    "reindexedAt": "2026-05-14T10:30:00"
  }
}
```

Error cases:

- `400`：文档没有 `extractedText`，或文档正在 `REINDEXING`。
- `401`：未登录。
- `404`：文档不存在或属于其他用户。
- `500`：重新分块或 embedding 失败，文档最终状态更新为 `REINDEX_FAILED`。

Security notes:

- 用户只能 reindex 自己的文档。
- response 不包含 `userId`、`filePath`、`storedFilename`、`embeddingJson`、`embedding_json`、`fullPrompt` 或异常堆栈。

### 获取 embedding 状态

- Method: `GET`
- Path: `/api/documents/{id}/embedding-status`
- Auth required: Yes

Response example:

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "documentId": 1,
    "status": "EMBEDDED",
    "chunkCount": 1,
    "embeddedChunkCount": 1,
    "embeddedAt": "2026-05-13T10:03:00",
    "updatedAt": "2026-05-13T10:03:00",
    "errorMessage": null
  }
}
```

### 相似 chunk 检索

- Method: `POST`
- Path: `/api/documents/search-chunks`
- Auth required: Yes
- Content-Type: `application/json; charset=utf-8`

Request body:

```json
{
  "query": "项目如何做用户隔离？",
  "topK": 5,
  "documentId": 1
}
```

`documentId` 可选。如果传入，先校验该文档属于当前用户。

Response example:

```json
{
  "code": 0,
  "message": "OK",
  "data": [
    {
      "chunkId": 10,
      "documentId": 1,
      "documentTitle": "demo.txt",
      "originalFilename": "demo.txt",
      "chunkIndex": 0,
      "score": 0.87,
      "contentPreview": "用户隔离通过 currentUserId 和 user_id 查询实现...",
      "embeddedAt": "2026-05-13T10:03:00"
    }
  ]
}
```

Error cases:

- `400`：`query` 为空，`topK` 不在 `1..20`。
- `404`：传入的 `documentId` 不属于当前用户。

## 7. RAG

### ask 问答

- Method: `POST`
- Path: `/api/chat/ask`
- Auth required: Yes
- Content-Type: `application/json; charset=utf-8`

Request body:

```json
{
  "question": "这个项目如何保护用户隐私？",
  "topK": 5,
  "documentId": 1,
  "conversationId": 2
}
```

字段说明：

- `question`: required。
- `topK`: optional，默认由 `RAG_DEFAULT_TOP_K` 决定，最大值由 `RAG_MAX_TOP_K` 决定。
- `documentId`: optional，用于限制在某个当前用户文档内检索。
- `conversationId`: optional，不传则创建新会话。

Response example:

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "conversationId": 2,
    "userMessageId": 11,
    "assistantMessageId": 12,
    "answer": "根据知识库片段 [S1]，项目通过 JWT、用户隔离和 AES-GCM 文件加密保护隐私。",
    "sources": [
      {
        "sourceId": "S1",
        "chunkId": 10,
        "documentId": 1,
        "documentTitle": "demo.txt",
        "originalFilename": "demo.txt",
        "chunkIndex": 0,
        "score": 0.87,
        "contentPreview": "项目通过 JWT、用户隔离和 AES-GCM 文件加密...",
        "embeddedAt": "2026-05-13T10:03:00"
      }
    ],
    "model": "deterministic",
    "provider": "deterministic",
    "usedTopK": 5
  }
}
```

Security notes:

- `sources` 是安全快照，不返回完整 prompt、embedding 数组或本地文件路径。
- `documentId` 和 `conversationId` 都会校验当前用户归属。

## 8. Conversations

### 会话列表

- Method: `GET`
- Path: `/api/conversations`
- Auth required: Yes

Response example:

```json
{
  "code": 0,
  "message": "OK",
  "data": [
    {
      "conversationId": 2,
      "title": "这个项目如何保护用户隐私？",
      "createdAt": "2026-05-13T10:04:00",
      "updatedAt": "2026-05-13T10:04:00",
      "messageCount": 2
    }
  ]
}
```

### 会话消息列表

- Method: `GET`
- Path: `/api/conversations/{id}/messages`
- Auth required: Yes

Response example:

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "conversationId": 2,
    "title": "这个项目如何保护用户隐私？",
    "messages": [
      {
        "messageId": 11,
        "role": "USER",
        "content": "这个项目如何保护用户隐私？",
        "sources": [],
        "createdAt": "2026-05-13T10:04:00"
      },
      {
        "messageId": 12,
        "role": "ASSISTANT",
        "content": "根据知识库片段 [S1]...",
        "sources": [
          {
            "sourceId": "S1",
            "chunkId": 10,
            "documentId": 1,
            "documentTitle": "demo.txt",
            "originalFilename": "demo.txt",
            "chunkIndex": 0,
            "score": 0.87,
            "contentPreview": "项目通过 JWT、用户隔离和 AES-GCM 文件加密..."
          }
        ],
        "createdAt": "2026-05-13T10:04:00"
      }
    ]
  }
}
```

Error cases:

- `404`：会话不存在或属于其他用户。

## 9. Audit Logs

### 查询当前用户审计日志

- Method: `GET`
- Path: `/api/me/audit-logs`
- Auth required: Yes
- Query params:
  - `page`: optional，默认 `0`
  - `size`: optional，默认 `20`
  - `action`: optional，例如 `LOGIN_SUCCESS`
  - `resourceType`: optional，例如 `DOCUMENT`
  - `success`: optional，`true` 或 `false`

Response example:

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "items": [
      {
        "id": 100,
        "action": "DOCUMENT_UPLOAD_SUCCESS",
        "resourceType": "DOCUMENT",
        "resourceId": 1,
        "success": true,
        "message": "Document uploaded",
        "ipAddress": "127.0.0.1",
        "userAgent": "PowerShell",
        "createdAt": "2026-05-13T10:05:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

Security notes:

- 只返回当前用户审计日志。
- 响应不包含 `userId`，message、IP、User-Agent 会脱敏和截断。

### 查询单条当前用户审计日志

- Method: `GET`
- Path: `/api/me/audit-logs/{id}`
- Auth required: Yes

Error cases:

- `404`：审计日志不存在或不属于当前用户。

PowerShell:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/me/audit-logs/<auditLogId>" `
  -Headers @{ Authorization = "Bearer $token" }
```

## 10. PowerShell 演示片段

```powershell
$baseUrl = "http://localhost:8080"

$registerBody = @{
  username = "demo_user"
  email = "demo_user@example.com"
  password = "Password123!"
} | ConvertTo-Json -Compress

Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/register" -ContentType "application/json; charset=utf-8" -Body $registerBody

$loginBody = @{
  username = "demo_user"
  password = "Password123!"
} | ConvertTo-Json -Compress

$loginResponse = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/login" -ContentType "application/json; charset=utf-8" -Body $loginBody
$token = $loginResponse.data.token

Invoke-RestMethod -Method Get -Uri "$baseUrl/api/documents" -Headers @{ Authorization = "Bearer $token" }
```

## 11. 敏感内容规则

- 不在示例中保存真实 JWT。
- 不在文档中保存真实 `JWT_SECRET`、`FILE_ENCRYPTION_KEY` 或数据库密码。
- 不保存真实本地隐私路径。
- 只使用 `<token>`、`<documentId>`、`<auditLogId>` 等占位符。
