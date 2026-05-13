# Troubleshooting

## Docker Desktop 未启动

现象：`docker compose up -d` 失败，提示无法连接 Docker daemon。

处理：

```powershell
docker info
```

如果命令失败，先启动 Docker Desktop，等待 Docker Engine 就绪后重新执行：

```powershell
docker compose up -d --build
```

## PostgreSQL 未 ready

现象：backend 启动失败或连接数据库失败。

处理：

```powershell
docker compose ps
docker compose logs postgres --tail=100
docker compose logs backend --tail=200
```

等待 `postgres` healthcheck 通过后，必要时重启 backend：

```powershell
docker compose restart backend
```

## 8080 端口冲突

现象：backend 端口绑定失败。

处理：在 `.env` 中设置其他端口，例如：

```env
APP_PORT=8081
```

然后重新启动：

```powershell
docker compose up -d
```

演示时把 `$baseUrl` 改为：

```powershell
$baseUrl = "http://localhost:8081"
```

## Maven 下载失败

现象：`.\mvnw.cmd test` 下载依赖失败或超时。

处理：

- 确认网络可访问 Maven Central。
- 稍后重试 `.\mvnw.cmd test`。
- 如果只是现场演示，可以先运行 Docker smoke 脚本展示主链路，再补跑 Maven 测试。

## PowerShell 中文乱码

现象：请求或响应中的中文显示异常。

处理：当前 PowerShell 会话先执行：

```powershell
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
```

JSON 请求显式使用：

```powershell
ContentType = "application/json; charset=utf-8"
```

## 注册 400 邮箱不能为空

原因：`RegisterRequest` 要求 `username`、`email`、`password`，注册示例漏传 `email` 会失败。

正确示例：

```powershell
$body = @{
  username = "demo_user"
  email = "demo_user@example.com"
  password = "Password123!"
} | ConvertTo-Json -Compress
```

## 登录 401

可能原因：

- 用户名或密码错误。
- 注册请求失败但继续登录。
- 后端重建后数据库数据被清空。

处理：

- 先确认注册响应 `code` 为 `0`。
- 登录只传 `username` 和 `password`，不要传 `email`。
- 使用新的用户名重新注册登录。

## token 为空

处理：

```powershell
$loginResponse
$token = $loginResponse.data.token
```

如果 `$token` 为空，检查登录响应是否为成功响应。不要继续执行上传、embedding 或 RAG，否则后续会出现 `401`。

## 上传 401

原因：未携带 JWT 或 `$token` 为空。

正确请求头：

```powershell
-Headers @{ Authorization = "Bearer $token" }
```

## 文件过大

默认最大文件大小由 `MAX_FILE_SIZE` 控制，当前示例为 `20971520` 字节。演示建议使用小型 `.txt` 文件。

如果需要调整，修改 `.env`：

```env
MAX_FILE_SIZE=20971520
```

然后重启 backend。

## embedding 失败

可能原因：

- 文档还没有 chunks。
- Ollama 未启动或模型未下载。
- embedding 维度与 `EMBEDDING_DIMENSION` 不匹配。

处理：

- 先确认上传响应状态为 `CHUNKED`，或调用 `/api/documents/{id}/chunks` 查看 chunks。
- 本地演示优先使用 `EMBEDDING_PROVIDER=deterministic`。
- 使用 Ollama 时确认模型输出维度与配置一致。

## Ollama 不可用

项目默认支持 deterministic provider，演示和自动化测试可以不依赖 Ollama：

```env
EMBEDDING_PROVIDER=deterministic
CHAT_PROVIDER=deterministic
```

如果使用 Ollama，确认本机服务可访问：

```powershell
ollama list
```

## pgvector 相关问题

Docker Compose 使用 `pgvector/pgvector:pg16`。如果 backend 日志提示 vector 扩展或类型不可用，确认没有换成普通 PostgreSQL 镜像：

```powershell
docker compose ps
docker compose logs postgres --tail=100
```

保留数据 volume 的情况下，可以重建镜像和容器：

```powershell
docker compose up -d --build
```

## RAG 没有 sources

可能原因：

- 文档没有执行 embedding。
- 查询内容和文档内容语义差距太大。
- `documentId` 指向了没有 embedded chunks 的文档。

处理：

```powershell
Invoke-RestMethod -Method Get -Uri "$baseUrl/api/documents/$docId/embedding-status" -Headers @{ Authorization = "Bearer $token" }
```

确认 `embeddedChunkCount` 大于 0 后重新 ask。
