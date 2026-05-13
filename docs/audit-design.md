# Secure Vault AI Audit Design

## 1. 设计目标

模块九的目标是在不改变模块一到模块八主链路的前提下，为 Secure Vault AI 增加可审计、可查询、用户隔离的安全事件记录能力。审计日志覆盖用户认证、文档上传/解析/embedding/删除、RAG 提问、跨用户访问失败和文件解密失败。

审计模块只记录安全摘要，不承担管理员后台、风控规则、集中日志平台或告警系统职责。

## 2. audit_logs 表设计

表名：`audit_logs`

字段：

- `id`：主键。
- `user_id`：事件所属用户 ID。登录失败等匿名事件可以为空。
- `action`：审计动作，使用字符串枚举。
- `resource_type`：资源类型，使用字符串枚举。
- `resource_id`：相关资源 ID，可为空。
- `success`：事件是否成功。
- `message`：安全摘要，最长 512。
- `ip_address`：请求 IP，最长 64。
- `user_agent`：请求 User-Agent，最长 255。
- `created_at`：创建时间。

索引：

- `idx_audit_logs_user_id`
- `idx_audit_logs_user_created_at`
- `idx_audit_logs_action`
- `idx_audit_logs_resource`
- `idx_audit_logs_success`
- `idx_audit_logs_created_at`

MVP 不设置强外键，只保留 `user_id` 数值，避免未来用户记录删除时破坏审计留存。

## 3. 审计事件类型

认证事件：

- `REGISTER_SUCCESS`
- `LOGIN_SUCCESS`
- `LOGIN_FAILURE`

文档事件：

- `DOCUMENT_UPLOAD_SUCCESS`
- `DOCUMENT_PARSE_SUCCESS`
- `DOCUMENT_PARSE_FAILURE`
- `DOCUMENT_EMBED_SUCCESS`
- `DOCUMENT_EMBED_FAILURE`
- `DOCUMENT_DELETE_SUCCESS`
- `DOCUMENT_DELETE_FAILURE`

RAG 与安全事件：

- `RAG_ASK_SUCCESS`
- `RAG_ASK_FAILURE`
- `RESOURCE_ACCESS_DENIED`
- `FILE_DECRYPT_FAILURE`
- `AUDIT_LOG_READ`

## 4. 用户隔离策略

审计查询接口只提供当前登录用户视角：

- `GET /api/me/audit-logs` 只查询 `user_id = currentUserId` 的记录。
- `GET /api/me/audit-logs/{id}` 使用 `id + currentUserId` 查询。
- 审计记录不存在或不属于当前用户时统一返回 `404`。

业务资源跨用户访问仍保持模块八的行为：对外返回 `404`，内部为访问者写入 `RESOURCE_ACCESS_DENIED`。

## 5. 脱敏策略

`AuditSanitizer` 会在写入和响应映射时处理审计内容，替换以下敏感内容：

- `Bearer` token 和 JWT 形态字符串。
- `password`、`rawPassword`、`FILE_ENCRYPTION_KEY`、`JWT_SECRET`、`encryptionKey`。
- `fullPrompt`、`embedding`、长向量数组。
- `filePath`、`storedFilename`、Windows 本地路径、`/uploads/`、`.secure-vault`、`file-encryption.key`。
- `jdbc:` 数据库连接串。
- Java 多行堆栈 trace。

审计日志不保存完整 prompt、完整 chunk、answer 原文、embedding 数组、文件路径、密钥、JWT、密码或完整异常堆栈。

## 6. 为什么审计失败不能影响主业务

审计日志是安全可观测性能力，不应成为登录、上传、删除、RAG 问答等主业务的可用性依赖。如果审计库写入短暂失败，主业务仍应按原结果返回。

当前实现使用独立事务模板写入审计日志，并在 `AuditLogService` 内部捕获所有写入异常。失败时只记录应用 `warn` 日志，不向 Controller 抛出异常。

## 7. 模块九验收方式

Maven 测试：

```powershell
cd C:\path\to\secure-vault-ai\backend
.\mvnw.cmd test
```

Docker 冒烟测试：

```powershell
cd C:\path\to\secure-vault-ai
powershell -ExecutionPolicy Bypass -File .\scripts\module9-smoke.ps1
```

验收重点：

- 用户只能查询自己的审计日志。
- 用户 B 通过 id 查询用户 A 的审计日志返回 `404`。
- 跨用户访问业务资源仍返回 `404`，并给访问者记录 `RESOURCE_ACCESS_DENIED`。
- 审计接口响应不包含 `userId`、密码、token、密钥、完整 prompt、embedding、文件路径或数据库连接串。
- 能记录 `REGISTER_SUCCESS`、`LOGIN_SUCCESS`、`DOCUMENT_UPLOAD_SUCCESS`、`DOCUMENT_EMBED_SUCCESS`、`RAG_ASK_SUCCESS`、`DOCUMENT_DELETE_SUCCESS`、`RESOURCE_ACCESS_DENIED`。
