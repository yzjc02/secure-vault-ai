# Secure Vault AI Backend (Module 1)

当前仅完成第一模块：**用户注册、登录、JWT 鉴权、当前用户识别**。

> 未完成文档上传、RAG、Ollama、向量检索、文件解析、pgvector 等后续模块。

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

## API（第一模块）

### 1) 注册

- **POST** `/api/auth/register`
- Body:

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "Password123"
}
```

### 2) 登录

- **POST** `/api/auth/login`
- Body（使用 `username + password`）:

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

### 3) 当前用户

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
