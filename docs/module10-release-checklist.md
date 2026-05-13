# Module 10 Release Checklist

## 1. 模块十目标

模块十用于完成 Secure Vault AI 的项目包装：强化 README、架构文档、API 使用文档、演示脚本、简历答辩材料、封版清单和静态验证脚本。在不修改后端主链路、不新增数据库结构、不新增依赖的前提下，让项目可以展示、可以讲解、可以放进简历。

## 2. 修改文件清单

本模块预期修改或新增：

- `README.md`
- `docs/architecture.md`
- `docs/api-guide.md`
- `docs/demo-script.md`
- `docs/resume-points.md`
- `docs/module10-release-checklist.md`
- `docs/troubleshooting.md`
- `scripts/module10-verify.ps1`

## 3. 不允许提交的文件清单

提交前确认以下文件或目录没有进入 Git：

- `.env`
- `.secure-vault/`
- `file-encryption.key` 的真实内容
- `data/uploads/`
- `backend/data/uploads/`
- Docker volume 导出的数据库数据
- 真实 JWT
- 真实 `JWT_SECRET`
- 真实 `FILE_ENCRYPTION_KEY`
- 真实数据库密码
- 真实本地隐私路径

## 4. 本地验证命令

```powershell
cd C:\path\to\secure-vault-ai
git status
git diff --stat
powershell -ExecutionPolicy Bypass -File .\scripts\module10-verify.ps1
```

后端测试：

```powershell
cd C:\path\to\secure-vault-ai\backend
.\mvnw.cmd test
```

## 5. Docker 验证命令

```powershell
cd C:\path\to\secure-vault-ai
docker compose up -d --build
docker compose ps
powershell -ExecutionPolicy Bypass -File .\scripts\module9-smoke.ps1
```

如果 Docker Desktop 未运行、端口冲突或 `.env` 缺失，不要修改后端代码绕过问题，先修复环境配置。

## 6. 文档一致性检查

- README 的项目定位、功能矩阵和测试说明与当前模块状态一致。
- `docs/api-guide.md` 中接口路径与当前 Controller 一致。
- 注册示例包含 `email`。
- 登录示例只包含当前 `LoginRequest` 要求的 `username` 和 `password`。
- 上传接口使用 `multipart/form-data`，字段为 `file` 和可选 `title`。
- RAG 接口为 `POST /api/chat/ask`。
- audit logs 接口为 `GET /api/me/audit-logs` 和 `GET /api/me/audit-logs/{id}`。
- 文档说明上传后会自动解析并自动分块，embedding 需要手动调用。

## 7. 安全检查

- 文档中没有真实 JWT。
- 文档中没有真实密钥。
- 文档中没有真实数据库密码。
- 文档中没有真实本地隐私路径。
- `.env.example` 只保留占位符或公开默认值。
- `file-encryption.key` 真实内容没有出现在 README 或 docs 中。
- API 示例使用 `<token>`、`<documentId>`、`<auditLogId>` 等占位符。

## 8. Git 提交前检查

```powershell
cd C:\path\to\secure-vault-ai
git status --short
git diff --stat
git diff -- README.md docs scripts/module10-verify.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\module10-verify.ps1
```

确认没有后端业务代码改动：

```powershell
git diff -- backend/src/main/java backend/src/main/resources backend/pom.xml
```

如果该命令有输出，需要确认是否属于本模块要求范围；模块十原则上不修改后端业务代码。

## 9. 建议 commit message

```text
docs: polish project documentation and demo materials
```

## 10. 建议 tag 名称

```text
module-10-project-packaging-complete
```

封版时由你手动执行 `git commit`、`git push` 和 `git tag`，本模块不自动提交。
