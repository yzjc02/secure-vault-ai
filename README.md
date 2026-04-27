# secure-vault-ai

Privacy-first personal knowledge vault powered by Spring Boot + RAG + Ollama.

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
