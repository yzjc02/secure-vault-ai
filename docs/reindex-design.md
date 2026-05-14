# Document Reindex Design

## 1. 功能目标

模块十一为 Secure Vault AI 增加手动文档重新索引能力。登录用户可以调用 `POST /api/documents/{documentId}/reindex`，让系统基于已有 `documents.extracted_text` 重新生成 chunks 和 embeddings，并把文档恢复到可检索的 `EMBEDDED` 状态。

这个能力只处理索引重建，不重新上传文件，不重新解析原始文件，不删除 Document 本体，也不新增前端或异步队列。

## 2. 为什么需要 Reindex

真实知识库系统中，索引不是一次性数据。以下场景都需要重建索引：

- chunk size、overlap 或清洗策略调整。
- embedding 模型、维度或 provider 切换。
- embedding 中途失败后需要恢复。
- 历史索引损坏或缺失。
- 系统升级后需要重建向量索引。
- 用户希望手动修复某个文档的检索质量。

Reindex 让系统具备数据生命周期管理能力，而不是只支持上传时的一次性处理。

## 3. API 设计

接口：

```http
POST /api/documents/{documentId}/reindex
Authorization: Bearer <token>
```

成功响应使用统一 `ApiResponse`：

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

响应对象是 `DocumentReindexResponse`，不是 JPA Entity。它不会返回 `userId`、`filePath`、`storedFilename`、`embeddingJson`、`embedding_json`、`fullPrompt` 或异常堆栈。

错误语义：

- `401`：未登录。
- `404`：文档不存在或不属于当前用户。
- `400`：文档没有 `extractedText`，或文档已经处于 `REINDEXING`。
- `500`：chunking 或 embedding 重建失败，前端只看到安全摘要。

## 4. 状态流转

新增文档状态：

- `REINDEXING`
- `REINDEX_FAILED`

成功流转：

```text
EMBEDDED / CHUNKED / PARSED / FAILED
-> REINDEXING
-> EMBEDDED
```

失败流转：

```text
REINDEXING
-> REINDEX_FAILED
```

如果文档没有 `extractedText`，不会进入 `REINDEXING`，直接返回 `400`。如果文档已经是 `REINDEXING`，直接返回 `400`，避免重复提交。

## 5. 旧 chunks 删除策略

Reindex 会删除当前用户当前文档的旧 chunks，然后基于已有 `extractedText` 重新分块。

删除方法位于 `DocumentChunkRepository`：

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("delete from DocumentChunk chunk where chunk.userId = :userId and chunk.documentId = :documentId")
int deleteByUserIdAndDocumentIdDirectly(@Param("userId") Long userId,
                                        @Param("documentId") Long documentId);
```

删除条件必须同时绑定 `userId` 和 `documentId`。返回的删除数量用于 audit log。

## 6. 为什么必须使用 bulk delete

旧 chunks 删除不能使用会加载实体的派生 delete 方法，例如仅依赖 Spring Data 方法名生成的 `deleteByUserIdAndDocumentId(...)`。

原因是 `DocumentChunk` 上有较大的 `embedding_json` 文本字段。此前 PostgreSQL/JPA 组合出现过 `Bad value for type long : [0.0045...]` 一类问题，本质是删除前加载实体时错误读取或映射了大向量文本字段。

模块十一必须使用 `@Modifying + @Query` bulk delete，直接在数据库执行 `delete from document_chunks where user_id = ? and document_id = ?` 语义，避免：

- 先 select 出所有 chunk。
- 加载 `embedding_json`。
- 触发 JPA 实体生命周期读取大字段。
- 因向量文本字段导致 PostgreSQL 类型读取异常。

## 7. 用户隔离设计

接口不接收 `userId`。`DocumentReindexService` 从 JWT 对应的 `CurrentUserService` 读取当前用户 ID，然后通过 `documentId + userId` 查询文档。

跨用户访问和不存在文档都返回 `404`，避免向用户暴露资源是否真实存在。旧 chunks 删除、重新保存 chunks、embedding 写入都绑定当前 `userId` 和 `documentId`。

RAG search 仍然只检索当前用户的 embedded chunks，因此 reindex 后不会破坏模块十 Evidence Trail 的用户隔离。

## 8. 事务与失败处理

Reindex 分为三个事务阶段：

1. 预检查并提交 `REINDEXING` 状态。
2. 在一个事务内 bulk delete 旧 chunks、重新分块、保存新 chunks、生成 embeddings、更新为 `EMBEDDED`。
3. 如果第二阶段失败，单独事务把文档更新为 `REINDEX_FAILED`。

这样做的原因是：如果所有操作都放在一个事务里，异常回滚会把 `REINDEX_FAILED` 状态也一起回滚，数据库最终可能看不到失败状态。

第二阶段失败时，删除和新 chunks 写入会随事务回滚，避免产生半成品或重复 chunks。随后第三阶段只保存安全失败状态和脱敏错误摘要。

## 9. Audit Log 设计

新增 audit action：

```text
DOCUMENT_REINDEX
```

成功和失败都会写 audit log，资源类型为 `DOCUMENT`，资源 ID 为 `documentId`。message 只包含安全元数据：

- `oldStatus`
- `newStatus`
- `deletedChunkCount`
- `newChunkCount`
- `embeddedChunkCount`
- 安全错误摘要

audit log 不记录完整 `extractedText`、完整 chunk 内容、`embedding_json`、完整 prompt、服务器文件路径、JWT、密钥或异常堆栈。

## 10. 测试策略

自动化测试覆盖：

- reindex 成功后返回 `documentId`、`title`、`status`、`chunkCount`、`embeddedChunkCount`、`reindexedAt`。
- reindex 成功后文档状态为 `EMBEDDED`。
- reindex 成功后 chunks 与 embeddings 数量大于 0。
- 连续 reindex 后 chunk 数稳定，不叠加旧 chunks。
- 用户 B reindex 用户 A 文档返回 `404`，且不改变用户 A 文档和 chunks。
- 未登录调用返回 `401`。
- 没有 `extractedText` 的文档返回 `400`，不创建 chunks。
- 文档处于 `REINDEXING` 时拒绝重复提交。
- embedding 失败时最终状态为 `REINDEX_FAILED`，并写失败 audit log。
- reindex response、RAG sources 和 chunk detail 不泄露敏感字段。
- reindex 后模块十 Evidence Trail 仍返回 `documentId`、`documentTitle`、`originalFilename`、`chunkIndex`、`score`、`snippet`、`createdAt`。

## 11. Smoke Test 使用方法

先启动后端：

```powershell
docker compose up -d --build
```

运行模块十一冒烟测试：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\module11-smoke.ps1
```

也可以指定地址：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\module11-smoke.ps1 -BaseUrl "http://localhost:8080"
```

脚本会自动注册两个唯一用户、上传文本、执行 embedding、提问、触发 reindex、再次提问、读取 chunk detail，并验证跨用户 reindex 和跨用户 chunk detail 都被拦截。

## 12. 面试讲解点

可以这样介绍模块十一：

我为 Secure Vault AI 设计并实现了文档重新索引机制。用户可以对已有文档触发 reindex，系统会基于已解析的 `extractedText` 删除旧 chunks，重新分块并重新生成 embeddings，最终恢复到可检索状态。这个功能解决了 chunk 策略变化、embedding 失败、模型更换和索引损坏后的数据恢复问题。

实现时我特别注意旧 chunks 删除不能加载大字段 `embedding_json`，而是通过 `@Modifying + @Query` 的 bulk delete 直接删除，避免 PostgreSQL/JPA 在大向量字段上的读取异常。同时接口做了严格的用户隔离，用户只能重建自己的文档索引，失败时会记录 `REINDEX_FAILED` 状态和审计日志。
