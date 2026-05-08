# Secure Vault AI Backend

当前后端已完成：

- 模块一：用户注册、登录、JWT 鉴权、当前用户识别
- 模块二：登录用户私有文档记录管理

> 暂未包含真实文件上传、文档解析、向量检索、Embedding、Ollama 和 RAG。

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

5. 访问后端接口，例如：

```powershell
curl.exe http://localhost:8080/api/documents
```

未携带 JWT 时，受保护接口应返回 `401`。

### Linux / macOS

```bash
chmod +x scripts/setup.sh
./scripts/setup.sh
docker compose up -d
```

### 本机配置与密钥说明

- `.env` 是本机私有配置，已经在 `.gitignore` 中忽略，不要提交。
- `.env.example` 只包含示例值，不包含真实密钥。
- `JWT_SECRET` 会由 setup 脚本自动生成，生成后不会打印到控制台。
- 生产环境应该使用服务器环境变量、CI/CD Secret 或 Docker secrets 注入密钥。
- 如果启动失败提示 `JWT_SECRET is required and must be at least 64 bytes...`，说明没有运行 setup 脚本，或生产环境没有配置 secret。

## 技术栈

- Java 17
- Spring Boot 4.0.6
- Spring Security
- Spring Data JPA
- JWT

## 启动前配置

JWT 密钥必须通过环境变量注入，**不要把真实 JWT_SECRET 提交到 GitHub**。

### Windows PowerShell

```powershell
$env:JWT_SECRET="replace-with-at-least-64-character-random-secret"
$env:JWT_EXPIRATION="86400000"
```

### macOS / Linux

```bash
export JWT_SECRET="replace-with-at-least-64-character-random-secret"
export JWT_EXPIRATION="86400000"
```

## 启动

```bash
cd backend
./mvnw spring-boot:run
```

Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

## 模块一：认证接口

### 注册

- **POST** `/api/auth/register`
- Body:

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "Password123"
}
```

### 登录

- **POST** `/api/auth/login`
- Body:

```json
{
  "username": "alice",
  "password": "Password123"
}
```

成功响应示例：

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

### 当前用户

- **GET** `/api/test/me`
- Header:

```text
Authorization: Bearer <JWT_TOKEN>
```

预期结果：

- 不带 token：`401 Unauthorized`
- 错误 token：`401 Unauthorized`
- 正确 token：`200 OK`

成功响应示例：

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

## 模块二：文档记录管理

模块二提供登录用户的私有文档记录 CRUD。所有文档操作都基于当前 JWT 认证身份解析出的 `userId`，前端不能传 `userId`，接口响应也不会返回 `userId`。

本模块暂不包含真实文件上传、文档解析、向量检索、Embedding、Ollama 和 RAG。

### 接口列表

- **POST** `/api/documents`：创建文档记录
- **GET** `/api/documents`：查询当前用户文档列表
- **GET** `/api/documents/{id}`：查询当前用户文档详情
- **PUT** `/api/documents/{id}`：修改当前用户文档
- **DELETE** `/api/documents/{id}`：删除当前用户文档

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

### 表结构参考

当前项目没有 Flyway/Liquibase，使用 JPA `ddl-auto: update` 自动建表。PostgreSQL 参考 SQL：

```sql
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_documents_user_id ON documents (user_id);
```
