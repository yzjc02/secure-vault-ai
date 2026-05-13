# Secure Vault AI Resume Points

## 1. 中文简历项目描述

### 精简版

Secure Vault AI：基于 Spring Boot 4、PostgreSQL + pgvector 的隐私优先个人知识库后端，支持文件加密存储、文档解析、embedding 检索、RAG 问答、用户隔离和审计日志。

### 标准版

Secure Vault AI 是一个隐私优先的本地个人知识库系统。本人负责后端主链路设计与实现，基于 Java 17、Spring Boot 4、Spring Security、Spring Data JPA、PostgreSQL + pgvector 完成用户注册登录、JWT 鉴权、私有文档 CRUD、文件上传、AES-GCM 加密存储、Apache Tika 文本解析、文本清洗与 chunking、embedding 向量存储、RAG 问答、conversation 记录、响应脱敏和 audit logs 审计日志，并通过 Maven 测试与 PowerShell smoke 脚本验证核心链路。

### 强化版

Secure Vault AI 是一个面向个人隐私文档的 AI 知识库后端项目。我将传统 Java 后端能力和 RAG 工程化结合，设计了从 JWT 鉴权、用户级数据隔离、文件加密落盘、Tika 文本抽取、chunking、embedding、pgvector 相似检索到 answer + sources 的完整链路；同时实现 `AccessControlService` 权限收口、跨用户访问返回 `404`、响应字段脱敏、AES-GCM 文件加密、audit logs 安全事件记录和审计脱敏。项目提供 Docker Compose、本地 deterministic provider、Maven 自动化测试和 smoke 测试，适合本地演示和面试讲解。

## 2. English Resume Descriptions

### Concise version

Secure Vault AI is a privacy-first personal knowledge vault built with Spring Boot 4, PostgreSQL, and pgvector. It supports JWT authentication, encrypted file storage, document parsing, chunking, embeddings, semantic search, RAG answers with sources, conversation history, user isolation, and audit logs.

### Detailed version

Built Secure Vault AI, a privacy-first local knowledge vault backend using Java 17, Spring Boot 4, Spring Security, Spring Data JPA, PostgreSQL, and pgvector. Implemented authentication with JWT and BCrypt, private document management, encrypted file upload with AES-GCM, Apache Tika text extraction, text cleaning and chunking, embedding generation, vector similarity search, RAG question answering with source citations, conversation persistence, response sanitization, user-level access control, and audit logging. Added Docker Compose deployment, deterministic test providers, Maven tests, and PowerShell smoke tests for reproducible local demos.

## 3. 技术亮点

- Spring Security + JWT：除注册登录外所有接口默认鉴权。
- BCrypt：用户密码不明文保存。
- 用户级数据隔离：文档、chunks、conversation、chat messages、audit logs 都按当前用户过滤。
- 文件上传与加密存储：上传文件经校验后使用 AES-GCM 加密落盘。
- Apache Tika 文档解析：支持 `pdf`、`docx`、`txt`、`md`、`markdown` 文本抽取。
- 文本清洗与 chunking：保留段落结构，使用 chunk size、overlap 和最小 chunk 参数。
- embedding + pgvector：支持 deterministic 和 Ollama provider，PostgreSQL 中进行向量相似检索。
- RAG answer + sources：问答返回答案和可追溯来源引用。
- conversation memory：持久化用户问题、助手回答和 sources 快照。
- AES-GCM：随机 IV、认证加密、本地透明解密读取。
- response sanitization：响应不暴露 `userId`、本地路径、stored filename、密钥、完整 prompt 或 embedding 数组。
- audit logs：记录认证、文档、embedding、RAG、访问拒绝和解密失败等安全事件。
- Docker Compose：一键启动后端与 pgvector PostgreSQL。
- Maven tests + smoke tests：自动化测试覆盖主链路，PowerShell smoke 验证 Docker 环境。

## 4. 面试讲解 Q&A

### 1. 为什么做这个项目？

我想做一个比普通 CRUD 更完整的后端项目。个人知识库适合展示文件处理、权限、安全和 AI 检索链路，同时隐私场景能自然引出加密、脱敏、审计和用户隔离这些后端工程能力。

### 2. 它和普通 CRUD 项目有什么区别？

普通 CRUD 主要是表单和数据库增删改查。这个项目在 CRUD 之外增加了文件上传、加密存储、文档解析、chunking、embedding、pgvector 检索、RAG 问答、会话记录、审计日志和安全边界，更接近真实后端系统。

### 3. RAG 是什么？

RAG 是 Retrieval-Augmented Generation。系统先根据用户问题检索相关知识片段，再把片段作为上下文交给模型生成答案。这样回答更贴近用户自己的文档，也可以返回 sources 做追溯。

### 4. embedding 是什么？

embedding 是把文本转成向量。语义相近的文本向量距离更近，因此可以用向量相似度从大量 chunks 里找到和问题最相关的内容。

### 5. pgvector 做什么？

pgvector 是 PostgreSQL 的向量扩展。它让项目直接在 PostgreSQL 里保存和检索向量，避免额外部署独立向量数据库，适合作品级 RAG 系统。

### 6. 为什么要 chunk？

文档通常很长，直接拿全文做 embedding 或放进 prompt 都不现实。chunking 可以把文档拆成较小片段，提高检索精度，也控制 RAG 上下文长度。

### 7. 如何保证用户隔离？

请求通过 JWT 得到当前用户，服务层不接收前端传入的 `userId`。所有文档、chunks、conversation、messages 和 audit logs 查询都按当前 `user_id` 过滤，关键资源访问通过 `AccessControlService` 校验归属。

### 8. 为什么跨用户访问返回 404？

如果返回 `403`，攻击者能知道资源 ID 存在，只是没有权限。返回 `404` 可以降低资源枚举风险，既表示拿不到资源，也不暴露它是否存在。

### 9. 为什么要文件加密？

个人知识库保存的是用户私有文件。即使上传目录或 Docker volume 被误共享，AES-GCM 加密也能避免文件以明文形式暴露。

### 10. AES-GCM 是什么？

AES-GCM 是一种认证加密模式，不仅加密内容，还能校验密文是否被篡改。项目使用随机 IV 和认证标签，读取文件时透明解密。

### 11. JWT 如何工作？

用户登录后后端签发 JWT。客户端后续请求携带 `Authorization: Bearer <token>`，过滤器校验签名和过期时间，通过后将用户身份放入 SecurityContext，业务层再读取当前用户。

### 12. 审计日志记录什么？

审计日志记录注册、登录、上传、解析、embedding、RAG、删除、跨用户访问失败、文件解密失败和审计读取等事件。它记录的是安全摘要，不记录敏感原文。

### 13. 审计日志为什么要脱敏？

审计日志经常用于排查问题，如果里面保存真实 token、密钥、完整 prompt、embedding 数组或本地路径，它本身就会成为泄露源，所以写入和返回都要脱敏。

### 14. 如果 Ollama 很慢怎么办？

项目默认提供 deterministic provider，测试和演示不依赖 Ollama。如果要真实模型，可以提前下载模型、调小 `topK`、限制上下文长度，或者把耗时任务改为异步队列，但当前作品阶段没有引入 MQ。

### 15. 如果文档很大怎么办？

当前通过文件大小限制、文本长度、chunk size 和 overlap 控制处理规模。后续可以做异步解析、批量 embedding、进度状态、分段读取和更细粒度的失败恢复。

### 16. 项目未来可以怎么优化？

可以做更完善的前端、异步任务、文件解析进度、向量索引优化、key rotation、用户级加密密钥、管理员审计视图、更多格式支持和模型评测。但这些不影响当前后端主链路演示。

### 17. 你在这个项目里最想强调哪一点？

我最想强调安全和 AI 工程化结合。RAG 本身不难，难点是把它放到一个有认证、权限、加密、脱敏、审计和测试的后端系统里。

## 5. STAR 法讲解

### 1 分钟版本

Situation：我想做一个能体现 Java 后端和 AI 应用能力的项目，而不是普通 CRUD。  
Task：目标是实现一个隐私优先的本地个人知识库，支持上传私有文档并进行 RAG 问答。  
Action：我用 Spring Boot 4 实现 JWT 鉴权、文档管理、AES-GCM 文件加密、Tika 解析、chunking、embedding、pgvector 检索、RAG answer + sources、conversation 记录和 audit logs；同时用 `AccessControlService` 做用户隔离，用 DTO 和审计脱敏避免敏感信息外泄。  
Result：项目可以通过 Docker Compose 本地启动，Maven 测试和 smoke 脚本验证主链路，能在面试中完整演示从注册登录到 RAG 问答和安全审计的流程。

### 3 分钟版本

Situation：在准备后端和 AI 应用开发实习时，我发现很多项目要么只是 CRUD，要么只是简单调用大模型 API，缺少工程化和安全边界。个人知识库场景天然涉及私有文件，所以我选择做 Secure Vault AI。

Task：我的目标是做一个隐私优先、可以本地运行、可以讲清楚数据流和安全链路的知识库后端。它需要支持用户注册登录、私有文档管理、文件上传、加密存储、文本解析、分块、embedding、向量检索、RAG 问答、会话记录和审计日志。

Action：技术上我使用 Java 17、Spring Boot 4、Spring Security、Spring Data JPA、PostgreSQL 和 pgvector。认证层使用 JWT 和 BCrypt；文件层上传后用 AES-GCM 加密落盘，读取时透明解密；解析层用 Apache Tika 抽取文本，再做清洗和 chunking；检索层用 deterministic 或 Ollama provider 生成 embedding，并用 pgvector 做相似 chunk 检索；RAG 层返回 answer + sources，并把用户消息和助手回答写入 conversations 和 chat_messages。安全上，所有资源都按当前用户 `user_id` 过滤，跨用户访问统一返回 `404`，响应不暴露 `userId`、本地路径、stored filename、密钥、完整 prompt 或 embedding 数组。审计层记录认证、文档、embedding、RAG 和访问拒绝事件，并对日志内容脱敏。

Result：项目形成了完整的后端作品闭环，既能用 Docker Compose 启动，也能通过 Maven tests 和 PowerShell smoke scripts 验证。面试时我可以演示注册登录、上传文档、自动解析分块、embedding、RAG 问答、sources、conversation、用户隔离和 audit logs，展示后端工程能力、安全意识和 AI 工程化能力。
