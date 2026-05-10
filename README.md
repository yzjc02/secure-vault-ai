# secure-vault-ai

Privacy-first personal knowledge vault powered by Spring Boot + RAG + Ollama.

## Backend Module 3: 真实文件上传与本地存储

模块三在现有文档 CRUD 基础上新增真实文件上传。登录用户可以上传 `pdf`、`docx`、`txt`、`md`、`markdown` 文件，后端会生成安全的 UUID 文件名，把文件保存到本地上传目录，并把文件元数据记录到 `documents` 表。

### 功能说明

- 上传接口：**POST** `/api/documents/upload`
- 认证方式：`Authorization: Bearer <token>`
- 请求类型：`multipart/form-data`
- 表单字段：
  - `file`：必填，上传文件。
  - `title`：可选，自定义标题；为空时默认使用原始文件名。
- 默认最大文件大小：`20MB`，可通过 `MAX_FILE_SIZE=20971520` 覆盖。
- 默认本地上传目录：`./data/uploads`，Docker 环境默认 `FILE_STORAGE_DIR=/app/data/uploads`。
- 删除文档时，如果该文档有关联上传文件，会同步删除本地文件。
- API 响应会返回 `originalFilename`、`storedFilename`、`fileType`、`fileSize`、`contentType` 等元数据，不会暴露服务器真实 `filePath`。

### Windows PowerShell 上传示例

```powershell
$token = "你的 JWT"
$form = @{
  file = Get-Item ".\test-files\demo.txt"
  title = "我的测试文档"
}

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/documents/upload" `
  -Method Post `
  -Headers @{ Authorization = "Bearer $token" } `
  -Form $form
```

### curl 上传示例

```powershell
curl.exe -X POST "http://localhost:8080/api/documents/upload" ^
  -H "Authorization: Bearer <token>" ^
  -F "file=@demo.txt" ^
  -F "title=我的测试文档"
```

### Docker Compose 持久化

Docker Compose 下 backend 服务会挂载 `uploads` volume 到 `/app/data/uploads`，容器重建后上传文件不会丢失。`postgres-data` volume 仍用于 PostgreSQL 数据持久化。

### 提交注意事项

- 不要提交 `.env`。
- 不要提交 `uploads/`、`data/uploads/`、`backend/data/uploads/` 等运行时上传目录。
- `.env.example` 只保留示例配置，可以提交。

## Backend Module 4: 文档解析与文本抽取

模块四在文件上传基础上新增文档文本抽取。登录用户上传 `pdf`、`docx`、`txt`、`md`、`markdown` 后，后端会同步解析文件、保存纯文本、记录文本长度和解析时间，并维护 `PARSING` / `PARSED` / `FAILED` 状态。解析失败只更新文档状态和 `errorMessage`，不会删除数据库记录或本地文件。

### 功能说明

- 支持 PDF、DOCX、TXT、Markdown 文本抽取。
- 上传成功后自动解析，`POST /api/documents/upload` 返回解析后的文档状态。
- 支持手动重新解析：**POST** `/api/documents/{id}/parse`。
- 支持查看完整解析文本：**GET** `/api/documents/{id}/text`。
- 列表和详情接口不会返回完整 `extractedText`，详情最多返回 `extractedTextPreview`。
- API 响应不会返回服务器真实 `filePath`。

### 手动重新解析

```powershell
curl.exe -X POST "http://localhost:8080/api/documents/1/parse" `
  -H "Authorization: Bearer <token>"
```

解析成功响应示例：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "id": 1,
    "title": "demo.txt",
    "status": "PARSED",
    "originalFilename": "demo.txt",
    "fileType": "txt",
    "textLength": 42,
    "parsedAt": "2026-05-10T16:00:00",
    "errorMessage": null
  }
}
```

解析失败响应示例：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "id": 1,
    "title": "demo.pdf",
    "status": "FAILED",
    "originalFilename": "demo.pdf",
    "fileType": "pdf",
    "textLength": 0,
    "parsedAt": "2026-05-10T16:01:00",
    "errorMessage": "未抽取到有效文本"
  }
}
```

### 获取完整解析文本

```powershell
curl.exe -X GET "http://localhost:8080/api/documents/1/text" `
  -H "Authorization: Bearer <token>"
```

成功响应示例：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "id": 1,
    "title": "demo.txt",
    "status": "PARSED",
    "originalFilename": "demo.txt",
    "fileType": "txt",
    "textLength": 42,
    "parsedAt": "2026-05-10T16:00:00",
    "extractedText": "这里是完整解析文本。"
  }
}
```

### 范围说明

- 本模块不做 chunking。
- 本模块不做 embedding。
- 本模块不做 RAG。
- 下一模块才进入文本分块。

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
