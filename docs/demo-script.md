# Secure Vault AI Demo Script

## 1. 演示目标

这套 10 到 15 分钟演示用于让面试官快速看到：

- 注册登录和 JWT 鉴权。
- 文件上传、AES-GCM 加密落盘、Apache Tika 文本解析。
- 文本清洗、chunking、embedding 和 pgvector 相似检索。
- RAG answer + sources 引用。
- conversation 记录和消息回看。
- 用户 B 访问用户 A 资源返回 `404`。
- 响应脱敏、文件加密和 audit logs。
- Docker Compose 可以一键启动后端和 PostgreSQL。

## 2. 演示前准备

PowerShell 建议先设置 UTF-8：

```powershell
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
```

启动服务：

```powershell
cd C:\path\to\secure-vault-ai
docker compose up -d --build
docker compose ps
```

准备演示变量：

```powershell
$baseUrl = "http://localhost:8080"
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
```

## 3. 10 到 15 分钟演示流程

### 第 1 分钟：项目定位

我现在演示的是一个隐私优先的本地个人知识库后端。它不是普通文档 CRUD，而是把认证、加密文件、解析、分块、embedding、pgvector、RAG、会话记录、用户隔离和审计日志串成完整链路。

证明能力：能把 Java 后端工程、安全设计和 AI 工程化结合起来。

面试官可能问：为什么做这个项目？

回答：个人知识库天然涉及隐私数据，普通云端 RAG 演示容易忽略权限、安全和可观测性。我选择本地优先方案，是为了展示从后端主链路到安全边界的完整实现。

### 第 2 分钟：架构说明

我现在展示 README 和 `docs/architecture.md`。请求先经过 Spring Security + JWT，服务层按当前用户做资源过滤；上传文件经 AES-GCM 加密落盘，解析后进入 chunks，embedding 后通过 pgvector 检索，RAG 返回 answer + sources，同时记录审计日志。

证明能力：能讲清 Controller、Service、Repository、Security、Document pipeline、RAG pipeline、Audit pipeline 的职责。

面试官可能问：为什么不用 Redis、MQ、Elasticsearch？

回答：当前目标是可运行的个人知识库 MVP。PostgreSQL + pgvector 足以支撑作品级语义检索，减少部署复杂度，更适合展示核心能力。

### 第 3 到 4 分钟：注册登录

```powershell
$userA = "demo_a_$timestamp"
$emailA = "$userA@example.com"
$password = "Password123!"

$registerA = @{
  username = $userA
  email = $emailA
  password = $password
} | ConvertTo-Json -Compress

Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/auth/register" `
  -ContentType "application/json; charset=utf-8" `
  -Body $registerA

$loginA = @{
  username = $userA
  password = $password
} | ConvertTo-Json -Compress

$loginResponseA = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/auth/login" `
  -ContentType "application/json; charset=utf-8" `
  -Body $loginA

$tokenA = $loginResponseA.data.token
```

我现在演示注册和登录。注册请求必须包含 `email`，登录成功后拿到 JWT。

证明能力：Spring Security、JWT、BCrypt、统一响应格式。

面试官可能问：JWT 怎么工作？

回答：登录成功后服务端签发 token，客户端后续在 `Authorization: Bearer <token>` 中携带。过滤器验证签名和过期时间，通过后把用户身份放入 SecurityContext，服务层再取当前用户 ID。

### 第 4 到 6 分钟：上传文档并查看状态

```powershell
$demoText = @"
Secure Vault AI protects private documents with JWT authentication, user-level isolation,
AES-GCM encrypted local file storage, Apache Tika parsing, chunking, embeddings,
pgvector semantic search, RAG answers with sources, conversation memory, and audit logs.
"@

$demoFile = Join-Path $env:TEMP "secure-vault-demo-$timestamp.txt"
Set-Content -Path $demoFile -Value $demoText -Encoding UTF8

$uploadResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/documents/upload" `
  -Headers @{ Authorization = "Bearer $tokenA" } `
  -Form @{ file = Get-Item $demoFile; title = "Secure Vault Demo" }

$docId = $uploadResponse.data.id
$uploadResponse.data
```

我现在上传一个文本文件。上传后系统会自动加密落盘、解析、分块，响应里可以看到状态、文件元数据、`textLength`、`chunkCount`。

证明能力：文件上传、AES-GCM、Tika、chunking、响应脱敏。

面试官可能问：怎么证明文件加密？

回答：业务读取统一通过 `FileStorageService` 透明解密，落盘内容带 `SVAIENC1` 加密头，不在 API 返回 `filePath` 或 `storedFilename`。设计细节在 `docs/privacy-design.md`。

### 第 6 到 8 分钟：embedding 和相似检索

```powershell
$embedResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/documents/$docId/embed" `
  -Headers @{ Authorization = "Bearer $tokenA" }

$embedResponse.data

$searchBody = @{
  query = "How does the project protect privacy?"
  topK = 5
  documentId = $docId
} | ConvertTo-Json -Compress

$searchResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/documents/search-chunks" `
  -Headers @{ Authorization = "Bearer $tokenA" } `
  -ContentType "application/json; charset=utf-8" `
  -Body $searchBody

$searchResponse.data
```

我现在触发 embedding，并用语义查询检索相似 chunk。

证明能力：embedding 生成、pgvector 检索、当前用户数据过滤。

面试官可能问：embedding 是什么？

回答：embedding 是把文本映射成向量，让语义相近的文本在向量空间里距离更近。RAG 前先用问题向量找相关 chunks，再把它们作为上下文给模型。

### 第 8 到 10 分钟：RAG ask，展示 answer + sources

```powershell
$askBody = @{
  question = "How does Secure Vault AI protect private documents?"
  topK = 5
  documentId = $docId
} | ConvertTo-Json -Compress

$askResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/chat/ask" `
  -Headers @{ Authorization = "Bearer $tokenA" } `
  -ContentType "application/json; charset=utf-8" `
  -Body $askBody

$askResponse.data.answer
$askResponse.data.sources
$conversationId = $askResponse.data.conversationId
```

我现在演示 RAG 问答。响应里有 `answer`，也有 `sources`，每个 source 对应命中的 chunk。

证明能力：RAG pipeline、来源引用、prompt 不外泄。

面试官可能问：sources 有什么价值？

回答：sources 能让回答可追溯，用户可以看到答案来自哪个文档和 chunk，降低黑盒感。

### 第 10 到 11 分钟：conversation

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/conversations" `
  -Headers @{ Authorization = "Bearer $tokenA" }

Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/conversations/$conversationId/messages" `
  -Headers @{ Authorization = "Bearer $tokenA" }
```

我现在展示会话列表和会话消息。RAG ask 会自动创建或复用 conversation，并保存用户问题、助手回答和 sources 快照。

证明能力：会话记忆、消息持久化、用户级隔离。

面试官可能问：sources 为什么存快照？

回答：这样即使文档后续删除或 chunk 变化，历史回答仍能保留当时的引用摘要，同时避免保存本地文件路径和敏感字段。

### 第 11 到 12 分钟：用户 B 访问用户 A 文档返回 404

```powershell
$userB = "demo_b_$timestamp"
$emailB = "$userB@example.com"

$registerB = @{
  username = $userB
  email = $emailB
  password = $password
} | ConvertTo-Json -Compress

Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/auth/register" `
  -ContentType "application/json; charset=utf-8" `
  -Body $registerB

$loginB = @{
  username = $userB
  password = $password
} | ConvertTo-Json -Compress

$loginResponseB = Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/auth/login" `
  -ContentType "application/json; charset=utf-8" `
  -Body $loginB

$tokenB = $loginResponseB.data.token

try {
  Invoke-RestMethod `
    -Method Get `
    -Uri "$baseUrl/api/documents/$docId" `
    -Headers @{ Authorization = "Bearer $tokenB" }
} catch {
  $_.Exception.Response.StatusCode.value__
}
```

我现在让用户 B 访问用户 A 的文档，预期返回 `404`。

证明能力：用户隔离、资源枚举防护、`AccessControlService` 权限收口。

面试官可能问：为什么不是 `403`？

回答：`403` 会告诉对方资源存在但没权限，`404` 更适合避免资源枚举。

### 第 12 到 13 分钟：查看 audit logs

```powershell
$auditResponse = Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/me/audit-logs?size=20" `
  -Headers @{ Authorization = "Bearer $tokenA" }

$auditResponse.data.items
$auditLogId = $auditResponse.data.items[0].id

Invoke-RestMethod `
  -Method Get `
  -Uri "$baseUrl/api/me/audit-logs/$auditLogId" `
  -Headers @{ Authorization = "Bearer $tokenA" }
```

我现在展示当前用户审计日志。可以看到注册、登录、上传、解析、embedding、RAG、删除或访问失败等安全事件。

证明能力：安全可观测性、审计脱敏、审计日志用户隔离。

面试官可能问：审计日志为什么要脱敏？

回答：审计日志本身也可能被查看或导出，如果里面保存 token、密钥、完整 prompt、文件路径，就会变成新的泄露点。

### 第 13 到 15 分钟：总结安全设计和工程亮点

我会总结三点：第一，完整后端链路，从上传到 RAG 可运行；第二，安全边界清楚，JWT、用户隔离、AES-GCM、响应脱敏和 audit logs 都落地；第三，工程化程度完整，有 Docker Compose、Maven tests、smoke tests 和模块十文档验证脚本。

证明能力：不是只会调模型 API，而是能把 AI 能力放进可维护、安全、可测试的后端系统。

## 4. 故障预案

| 问题 | 处理方式 |
| --- | --- |
| Docker 没启动 | 打开 Docker Desktop，确认 `docker info` 可用后重新执行 `docker compose up -d --build` |
| PostgreSQL 未 ready | 执行 `docker compose ps` 和 `docker compose logs postgres --tail=100`，等待 healthcheck 通过 |
| 端口 8080 被占用 | 在 `.env` 设置 `APP_PORT=8081` 后重新 `docker compose up -d`，演示变量改为 `http://localhost:8081` |
| Ollama 不可用 | 使用默认 `EMBEDDING_PROVIDER=deterministic` 和 `CHAT_PROVIDER=deterministic`，演示不依赖 Ollama |
| Maven 下载慢 | 先确认网络和 Maven 本地缓存；必要时只演示 Docker smoke，Maven 测试稍后补跑 |
| 中文乱码 | PowerShell 先执行 `$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)`，请求使用 `application/json; charset=utf-8` |
| 注册 `400` 邮箱不能为空 | 注册请求体必须包含 `email`，示例使用 `email = "$user@example.com"` |
| token 为空 | 检查登录响应 `code` 是否为 `0`，用户名密码是否和注册一致，读取 `$loginResponse.data.token` |
| 上传 `401` | 确认请求头是 `Authorization = "Bearer $tokenA"`，且 `$tokenA` 非空 |
| 文件过大 | 当前默认 `MAX_FILE_SIZE=20971520`，演示使用小型 `.txt` 文件 |
| embedding 维度不匹配 | deterministic 默认维度跟配置一致；如果切换 Ollama，确认 `EMBEDDING_DIMENSION` 与模型输出维度一致 |
| RAG 没有 sources | 先确认文档状态为 `EMBEDDED`，再执行 `/api/chat/ask` |
| audit logs 为空 | 先执行注册、登录、上传、embedding 或 RAG 操作，再查询 `/api/me/audit-logs?size=20` |

## 5. 结束语

这个项目的亮点不是功能堆叠，而是把每一步都做成可解释、可验证、可隔离的后端链路。面试时优先讲清楚数据怎么流动、权限怎么收口、敏感信息怎么避免泄露。
