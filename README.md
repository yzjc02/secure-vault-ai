# secure-vault-ai

Privacy-first personal knowledge vault powered by Spring Boot + RAG + Ollama.

## 快速启动

### Windows

1. 安装 Docker Desktop。
2. 克隆项目并进入项目根目录。
3. 执行初始化脚本：

```powershell
.\scripts\setup.ps1
```

4. 启动服务：

```powershell
docker compose up -d
```

如果修改过 Dockerfile、依赖或 Compose 配置，建议重建：

```powershell
docker compose down
docker compose up -d --build
docker compose ps
docker compose logs backend --tail=200
```

5. 访问后端接口，例如：

```powershell
curl.exe -i http://localhost:8080/api/documents
```

未携带 JWT 时，受保护接口应返回 `401`。

### Linux / macOS

```bash
chmod +x scripts/setup.sh
./scripts/setup.sh
docker compose up -d
```

重建和查看日志：

```bash
docker compose down
docker compose up -d --build
docker compose ps
docker compose logs backend --tail=200
```

### 本机配置与密钥说明

- `.env` 是本机私有配置，已经在 `.gitignore` 中忽略，不要提交。
- `.env.example` 只包含示例值，不包含真实密钥。
- `JWT_SECRET` 会由 setup 脚本自动生成，生成后不会打印到控制台。
- 生产环境应该使用服务器环境变量、CI/CD Secret 或 Docker secrets 注入密钥。
- 如果启动失败提示 `JWT_SECRET is required and must be at least 64 bytes...`，说明没有运行 setup 脚本，或生产环境没有配置 secret。

## Windows PowerShell 接口测试建议

复杂 JSON 或包含中文的请求，推荐使用 PowerShell 7，并先设置控制台和管道输出为 UTF-8：

```powershell
chcp 65001
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
```

复杂 JSON 或包含中文的请求，推荐使用 `Invoke-WebRequest` / `Invoke-RestMethod`，请求体用 `ConvertTo-Json -Compress` 生成后转为 UTF-8 bytes，并显式指定 `application/json; charset=utf-8`。

```powershell
$registerBody = @{
  username = "alice"
  email = "alice@example.com"
  password = "Password123"
} | ConvertTo-Json -Compress

Invoke-WebRequest `
  -Method POST `
  -Uri "http://localhost:8080/api/auth/register" `
  -ContentType "application/json; charset=utf-8" `
  -Body $registerBody

$loginBody = @{
  username = "alice"
  password = "Password123"
} | ConvertTo-Json -Compress

$loginResponse = Invoke-RestMethod `
  -Method POST `
  -Uri "http://localhost:8080/api/auth/login" `
  -ContentType "application/json; charset=utf-8" `
  -Body $loginBody

$token = $loginResponse.data.token

$documentBody = @{
  title = "Spring Security 学习笔记"
  description = "记录模块一 JWT 鉴权流程"
} | ConvertTo-Json -Compress

$documentBytes = [System.Text.Encoding]::UTF8.GetBytes($documentBody)

Invoke-RestMethod `
  -Method POST `
  -Uri "http://localhost:8080/api/documents" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json; charset=utf-8" `
  -Body $documentBytes
```

不推荐在复杂 JSON 场景下使用 `curl.exe -d $body`，PowerShell 可能会影响参数传递或多行 JSON，导致后端收到的请求体不是预期 JSON。

如果看到 `å`、`è`、`ç` 等字符，通常表示 UTF-8 字节被错误按 Latin-1 或 Windows-1252 解码。后端已经强制请求/响应 UTF-8，并有自动化测试覆盖中文创建、列表读取和错误响应。PostgreSQL 新库默认应为 UTF8，可用以下命令检查：

```powershell
docker compose exec postgres psql -U securevault_user -d securevault -c "SHOW SERVER_ENCODING;"
```

## Backend Module 1: 用户注册 / 登录 / JWT 鉴权

后端目录：`backend`

> 注意：当前仅完成第一模块（用户注册、登录、JWT 鉴权、当前用户识别），未完成文档上传、RAG、Ollama、向量检索等后续模块。

### 0) 配置 JWT 环境变量（必须）

JWT 密钥必须通过环境变量提供，**不要把真实 JWT_SECRET 提交到 GitHub**。

Windows PowerShell:

```powershell
$env:JWT_SECRET="replace-with-at-least-64-character-random-secret"
$env:JWT_EXPIRATION="86400000"
```

macOS / Linux:

```bash
export JWT_SECRET="replace-with-at-least-64-character-random-secret"
export JWT_EXPIRATION="86400000"
```

### 1) 启动项目

```bash
cd backend
./mvnw spring-boot:run
```

Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

### 2) 注册用户

- **POST** `http://localhost:8080/api/auth/register`
- Body (JSON):

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "Password123"
}
```

### 3) 登录并获取 JWT

- **POST** `http://localhost:8080/api/auth/login`
- Body (JSON):

```json
{
  "username": "alice",
  "password": "Password123"
}
```

成功后会返回：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "token": "<JWT_TOKEN>",
    "tokenType": "Bearer"
  }
}
```

### 4) 使用 JWT 访问受保护接口

- **GET** `http://localhost:8080/api/test/me`
- Header:

```text
Authorization: Bearer <JWT_TOKEN>
```

结果预期：
- 不带 Token：`401 Unauthorized`
- Token 非 `Bearer <token>` 格式：`401 Unauthorized`
- Token 无效或过期：`401 Unauthorized`
- 带有效 Token：返回当前登录用户信息

示例成功响应：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "userId": 1,
    "username": "alice",
    "authorities": [
      { "authority": "ROLE_USER" }
    ]
  }
}
```

### 5) Postman 测试建议

1. 新建 Collection：`Secure Vault Auth`。
2. 建立 `register` 请求（POST），先注册用户。
3. 建立 `login` 请求（POST），提取返回 `data.token` 存到环境变量 `jwt_token`。
4. 建立 `me` 请求（GET），Header 设置：
   - `Authorization: Bearer {{jwt_token}}`
5. 先不带 Header 调一次验证 401，再带 Header 验证成功。

## Backend Module 2: 文档记录管理

模块二新增登录用户的私有文档记录 CRUD。所有文档操作都基于当前 JWT 认证身份解析出的 `userId`，前端不能传 `userId`，接口响应也不会返回 `userId`。

本模块暂不包含真实文件上传、文档解析、向量检索、Embedding、Ollama 和 RAG。

### 接口列表

- **POST** `http://localhost:8080/api/documents`：创建文档记录
- **GET** `http://localhost:8080/api/documents`：查询当前用户文档列表
- **GET** `http://localhost:8080/api/documents/{id}`：查询当前用户文档详情
- **PUT** `http://localhost:8080/api/documents/{id}`：修改当前用户文档
- **DELETE** `http://localhost:8080/api/documents/{id}`：删除当前用户文档

### 认证方式

```text
Authorization: Bearer <token>
```

### 创建文档

```powershell
curl.exe -X POST "http://localhost:8080/api/documents" `
  -H "Authorization: Bearer <token>" `
  -H "Content-Type: application/json" `
  -d "{\"title\":\"Spring Security 学习笔记\",\"description\":\"记录模块一 JWT 鉴权流程\"}"
```

### 查询文档列表

```powershell
curl.exe -X GET "http://localhost:8080/api/documents" `
  -H "Authorization: Bearer <token>"
```

### 查询文档详情

```powershell
curl.exe -X GET "http://localhost:8080/api/documents/1" `
  -H "Authorization: Bearer <token>"
```

### 修改文档

```powershell
curl.exe -X PUT "http://localhost:8080/api/documents/1" `
  -H "Authorization: Bearer <token>" `
  -H "Content-Type: application/json" `
  -d "{\"title\":\"新的标题\",\"description\":\"新的描述\"}"
```

### 删除文档

```powershell
curl.exe -X DELETE "http://localhost:8080/api/documents/1" `
  -H "Authorization: Bearer <token>"
```

### 权限隔离说明

文档详情、修改、删除都会使用 `documentId + currentUserId` 查询。文档不存在或文档不属于当前登录用户时，统一返回 `404` 和 `文档不存在`，避免资源枚举。
