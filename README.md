# secure-vault-ai

Privacy-first personal knowledge vault powered by Spring Boot + RAG + Ollama.

## Backend Module 1: 用户注册 / 登录 / JWT 鉴权

后端目录：`backend`

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
- 带有效 Token：返回当前登录用户信息

### 5) Postman 测试建议

1. 新建 Collection：`Secure Vault Auth`。
2. 建立 `register` 请求（POST），先注册用户。
3. 建立 `login` 请求（POST），提取返回 `data.token` 存到环境变量 `jwt_token`。
4. 建立 `me` 请求（GET），Header 设置：
   - `Authorization: Bearer {{jwt_token}}`
5. 先不带 Header 调一次验证 401，再带 Header 验证成功。
