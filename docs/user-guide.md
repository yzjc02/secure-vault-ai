# Secure Vault AI User Guide

Secure Vault AI 是一个本地优先、隐私优先的个人知识库雏形。它把用户注册登录、JWT 鉴权、文档上传、AES-GCM 加密存储、文本解析、chunking、embedding、pgvector 检索、RAG 问答、sources 引用、会话记录和 audit logs 串成一条可在浏览器里使用的主链路。

## 现在可以怎么使用

当前版本提供一个极简 Chat 风格 Web UI。你可以在浏览器中完成 Register、Login、Upload、Embed、Ask、查看 answer、查看 sources、查看 audit logs、刷新会话、删除文档等操作。

页面只使用 Spring Boot 静态资源能力和原生 HTML、CSS、JavaScript，不需要 Node、npm、Vue、React、Vite 或外部 CDN。

## 中英文切换

页面支持中文和 English，可以在侧边栏顶部切换语言。

语言偏好会自动保存到浏览器 localStorage。刷新页面后会恢复上次选择的语言，切换语言不会影响登录状态、已上传文档、会话列表、聊天内容或 audit logs。

语言切换只影响 UI 文案、按钮、占位符、提示信息、状态标签和静态说明。AI 回答、用户上传的文档内容、source preview、文档标题、文件名和后端返回的 audit action 枚举不会被自动翻译。

## 一键启动

在项目根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-vault.ps1
```

第一次启动时脚本会从 `.env.example` 复制生成本地 `.env`，并自动生成本地 `JWT_SECRET`、`FILE_ENCRYPTION_KEY` 和本地数据库密码。脚本不会把生成的密钥打印到控制台。

启动完成后访问：

```text
http://localhost:8080
```

## 停止

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-vault.ps1
```

## 查看日志

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\logs-vault.ps1
```

日志脚本会跟随 backend 容器日志，用于排查启动失败、数据库连接失败、上传失败、embedding 失败和 RAG 异常。

## 第一次使用流程

1. 打开 `http://localhost:8080`。
2. 在 Register 表单注册账号，必须填写 username、email、password。
3. 在 Login 表单使用 username 和 password 登录。
4. 点击 Upload，选择 `txt`、`pdf`、`docx`、`md` 或 `markdown` 文件，可选填写 title。
5. 上传成功后在左侧 Knowledge Files 找到文档，点击 Embed。
6. 在底部聊天框输入问题，必要时调整 topK。
7. 点击 Ask，查看 answer。
8. 在回答下方查看 sources，确认回答来自哪个 documentId、chunkIndex 和内容预览。
9. 点击 Audit Logs 查看最近的 audit logs。
10. 不再需要某个文档时，在文档项上点击 Delete。

## RAG Sources 怎么看

RAG answer 下方的 Sources 表示系统检索到的相关文档片段。每个 source 会显示：

- 文档名
- documentId
- chunk 位置
- 匹配度
- 文本预览

Sources 不是模型随便编的引用，而是来自 pgvector 检索命中的 document chunk。通过文本预览可以判断回答是否真的有依据。

如果 sources 显示 “No readable preview was returned”，说明当前后端没有返回可读片段，需要检查 RAG source DTO。

## 推荐上传资料

- 课程笔记
- 项目文档
- 面试题整理
- Java / Spring 学习资料
- `txt`、`pdf`、`docx`、`md` 文件

## 常见问题

### Docker Desktop 没启动

现象：`start-vault.ps1` 输出 Docker Desktop is not running。

处理：启动 Docker Desktop，等待 Docker Engine 就绪后重新执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-vault.ps1
```

### 8080 端口被占用

现象：backend 容器启动失败或浏览器打不开 `http://localhost:8080`。

处理：先查看日志确认原因：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\logs-vault.ps1
```

如需改端口，可在本地 `.env` 调整 `APP_PORT`，然后重新启动。默认浏览器 UI 演示仍以 `http://localhost:8080` 为准。

### 注册失败

注册请求必须包含 email。确认 Register 表单里的 username、email、password 都已填写，email 格式正确，password 长度满足后端校验。

### 登录失败

确认先完成 Register，Login 只需要 username 和 password。不要把 email 当作 username 登录。

### token 过期

页面收到 401 时会清除本地 token 并显示 Login 区域。重新登录即可。

### 上传失败

确认已经登录，文件非空，文件扩展名属于当前后端支持范围，并且文件大小没有超过 `.env` 中的 `MAX_FILE_SIZE`。

### embedding 失败

确认上传后的文档状态已成功解析并分块，通常状态为 `CHUNKED`。本地演示推荐保持默认 `EMBEDDING_PROVIDER=deterministic`。

### RAG 没回答

先确认至少有一个文档已执行 Embed，状态为 `EMBEDDED`，再提问。如果限定了某个文档范围，确认该文档属于当前用户并且已有 embedded chunks。

### audit logs 为空

新账号刚注册后日志可能较少。先执行 Login、Upload、Embed、Ask 或 Delete，再刷新 Audit Logs。

### 页面能打开但接口 401

说明静态页面可访问，但业务接口仍需要 JWT。请先 Login。未登录访问 `/api/**` 返回 401 是预期行为。

## 安全提醒

- 不要提交 `.env`。
- 不要泄露 token。
- 不要提交 `.secure-vault`。
- 不要提交 `file-encryption.key`。
- 不要把真实 `JWT_SECRET`、`FILE_ENCRYPTION_KEY` 或数据库密码写进文档、Issue 或提交信息。
- UI 不会显示 token 明文，也不会把 password、token、secret 或 file encryption key 打印到浏览器控制台。
- 本地知识库数据保存在本机和 Docker volume 中，删除容器不等于删除所有 volume 数据。
