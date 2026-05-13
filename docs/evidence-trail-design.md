# RAG Evidence Trail Design

## 1. 功能目标

模块十把 RAG 问答从“返回答案和简单来源”增强为“答案可追溯、来源可验证”。`POST /api/chat/ask` 返回的每个 source 都包含文档、chunk、相似度、snippet 和创建时间，用户可以继续调用 chunk detail API 查看完整来源片段。

## 2. 为什么需要 Evidence Trail

RAG 答案如果只有自然语言结论，用户很难判断答案是否来自自己的资料、是否检索到了正确文档、是否引用了合适片段。Evidence Trail 让前端和用户都能从 answer 回溯到具体 document 与 chunk，降低幻觉风险，也方便演示用户隔离和安全脱敏能力。

## 3. RAG Source 字段说明

`sources` 中每个元素至少包含：

- `documentId`：来源文档 ID。
- `documentTitle`：来源文档标题。
- `originalFilename`：用户上传时的原始文件名。
- `chunkIndex`：来源 chunk 序号。
- `score`：语义检索相似度分数。
- `snippet`：经过空白归一化和长度截断的来源片段。
- `createdAt`：chunk 创建时间；如果 chunk 时间为空，则使用文档创建时间。

为兼容旧响应，当前仍保留 `sourceId`、`chunkId`、`contentPreview` 和 `embeddedAt`，但不会返回 embedding、文件路径、`userId` 或 prompt。

## 4. Chunk Detail API 设计

接口：

```http
GET /api/documents/{documentId}/chunks/{chunkIndex}
Authorization: Bearer <token>
```

成功响应使用统一 `ApiResponse`：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "documentId": 31,
    "documentTitle": "项目计划书",
    "originalFilename": "secure-vault-ai-plan.pdf",
    "chunkIndex": 11,
    "content": "完整 chunk 内容",
    "textLength": 356,
    "createdAt": "2026-05-13T10:20:00"
  }
}
```

返回对象是 `DocumentChunkDetailResponse`，不是 `DocumentChunk` 实体。

## 5. 用户隔离设计

详情查询先通过 `AccessControlService.requireOwnedDocument(documentId, currentUserId)` 校验文档归属，再调用 `DocumentChunkRepository.findByUserIdAndDocumentIdAndChunkIndex(...)` 查询 chunk。查询条件绑定当前登录用户、文档 ID 和 chunk 序号，避免只凭 `documentId + chunkIndex` 读取其他用户数据。

跨用户访问和不存在的资源返回 `404`，不向调用方暴露资源是否真实存在。未登录访问由 Spring Security 返回 `401`。

## 6. 敏感字段禁止返回

RAG source 和 chunk detail 都只返回安全 DTO。禁止返回：

- `embedding`
- `embeddingJson`
- `embedding_json`
- `filePath`
- `storedFilename` 的服务器内部路径
- `userId`
- `fullPrompt`
- 异常堆栈

snippet 会归一化连续空白并按 code point 截断，避免中文乱码和直接暴露完整 chunk。

## 7. Audit Log 设计

chunk detail 成功或失败都会记录 `DOCUMENT_CHUNK_VIEW`，资源类型为 `DOCUMENT_CHUNK`。日志 message 只记录 `documentId`、`chunkIndex` 和成功状态相关信息，不记录完整 chunk、embedding、大段 answer 或 prompt。跨用户访问文档归属校验失败时，复用已有 `RESOURCE_ACCESS_DENIED` 审计链路。

## 8. 测试策略

自动化测试覆盖：

- RAG ask 返回非空 answer 和非空 sources。
- source 包含 `documentId`、`documentTitle`、`originalFilename`、`chunkIndex`、`score`、`snippet`、`createdAt`。
- source 和 chunk detail 不包含 embedding、文件路径、`userId`、`fullPrompt`。
- 用户 A 可以访问自己的 chunk detail。
- 用户 B 访问用户 A 的 chunk detail 返回 `404`。
- 不存在 chunkIndex 返回 `404`。
- 未登录访问 chunk detail 返回 `401`。

## 9. Smoke Test 使用方式

先启动后端：

```powershell
docker compose up --build
```

然后运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\module10-smoke.ps1
```

也可以指定地址：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\module10-smoke.ps1 -BaseUrl "http://localhost:8080"
```

脚本会自动注册两个唯一用户、上传文本、执行 embedding、提问、检查 source 字段、读取 chunk detail，并验证跨用户访问被拦截。

## 10. 面试讲解点

- Evidence Trail 解决的是 RAG 答案可信度和可验证性问题。
- source metadata 让 answer 能回溯到具体文档和 chunk。
- snippet 是安全预览，chunk detail 是登录后按归属查询的完整证据。
- 权限控制使用“当前用户 + documentId + chunkIndex”三元条件，不依赖前端可信输入。
- 安全 DTO 和审计脱敏共同避免 embedding、路径、`userId`、prompt 泄露。
