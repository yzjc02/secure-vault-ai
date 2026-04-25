# Secure Vault AI

Secure Vault AI is a backend project for a secure document vault with AI-assisted features planned for later stages.

## Current Backend Module

The current completed backend module focuses on authentication and authorization:

- User registration
- User login
- JWT-based authentication

The project does not currently claim completed document upload, document parsing, vector search, or RAG features.

## Tech Stack

- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT

## Local Setup

### Prerequisites

- Java 17
- Maven, or the included Maven wrapper
- PostgreSQL

### 1. Create the Database

Run the following SQL in PostgreSQL:

```sql
CREATE DATABASE secure_vault_ai;

CREATE USER secure_vault_user WITH PASSWORD 'change_me';

GRANT ALL PRIVILEGES ON DATABASE secure_vault_ai TO secure_vault_user;

\c secure_vault_ai

GRANT ALL ON SCHEMA public TO secure_vault_user;
```

### 2. Configure the Backend

Update `src/main/resources/application.properties` with your local database credentials and JWT settings.

Example:

```properties
spring.application.name=backend

spring.datasource.url=jdbc:postgresql://localhost:5432/secure_vault_ai
spring.datasource.username=secure_vault_user
spring.datasource.password=change_me

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

jwt.secret=replace_with_a_strong_secret_key
jwt.expiration=86400000
```

### 3. Run the Application

On macOS or Linux:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

By default, the backend runs at:

```text
http://localhost:8080
```

## API Examples

The examples below assume the authentication endpoints are exposed under `/api/auth`.

### Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "demo_user",
    "email": "demo@example.com",
    "password": "StrongPassword123!"
  }'
```

Example response:

```json
{
  "message": "User registered successfully"
}
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@example.com",
    "password": "StrongPassword123!"
  }'
```

Example response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Access a Protected Endpoint with JWT

Use the token returned by the login endpoint as a Bearer token.

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Replace `/api/users/me` with any protected endpoint in the backend.

## Planned Features

- Document upload
- Document parsing
- Vector search
- RAG question answering
- Docker deployment

## Project Status

This repository is currently focused on the backend authentication foundation. Future modules will extend the system into secure document management and AI-assisted retrieval.
