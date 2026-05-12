# Secure Vault AI Privacy Design

## Data Ownership Model

Secure Vault AI treats each authenticated user as an isolated data owner. Documents, chunks, conversations, and chat messages carry a persisted `userId`, but controllers and request DTOs do not accept `userId` from clients. Services derive the current user from Spring Security/JWT and scope all resource reads and writes to that user.

Cross-user access returns `404` instead of `403`. This avoids disclosing whether a document, chunk, conversation, or message exists under another account.

## Encrypted File Storage

Uploaded source files are encrypted before they are written to local storage. The encryption format is:

```text
SVAIENC1 + ivLength + iv + ciphertextWithGcmTag
```

The implementation uses JDK cryptography only:

- Cipher: `AES/GCM/NoPadding`
- IV: random 12 bytes per encryption
- Tag: 128 bit
- Key: Base64 32-byte AES key, or a 32+ character UTF-8 string derived with SHA-256

The encryption key is configured with `FILE_ENCRYPTION_KEY`. In non-production local development, an empty key creates `.secure-vault/file-encryption.key`; this path is ignored by git. Production profiles must configure a stable key explicitly.

Legacy plaintext files are supported: if stored bytes do not start with `SVAIENC1`, the file is treated as a pre-module-eight plaintext file and read as-is.

## Access Control Boundary

`AccessControlService` centralizes ownership checks:

- `requireOwnedDocument(documentId)`
- `requireOwnedDocument(documentId, currentUserId)`
- `requireOwnedConversation(conversationId)`
- chunk access through current-user scoped repositories or document ownership

Document upload, listing, detail, delete, text extraction, chunking, chunk listing, embedding, embedding status, semantic search, RAG ask, conversation listing, and message reads all use current-user ownership checks.

## Cross-User Isolation

User B cannot read, delete, parse, chunk, embed, search, ask against, or view conversations for User A data. Search is scoped to current-user embedded chunks. If a request includes a `documentId` or `conversationId`, ownership is verified before further processing.

## RAG Prompt Privacy Boundary

RAG prompt construction may use retrieved chunk content internally, but API responses expose only safe source snapshots:

- `sourceId`
- `chunkId`
- `documentId`
- `documentTitle`
- `originalFilename`
- `chunkIndex`
- `score`
- `contentPreview`
- `embeddedAt`

The full prompt is not returned. Chat message history stores user/assistant message content and safe source JSON only.

## API Response Safety

Public API responses must not include:

- `userId`
- `filePath`
- `storedFilename`
- embedding arrays or `embedding*` metadata fields
- `fullPrompt`
- encryption keys or key ids
- JDBC URLs
- local absolute upload paths
- JWT values
- Java stack traces

Business errors are returned through the existing `ApiResponse` envelope with safe summary messages.

## Log Redaction

The backend does not log encryption keys, JWTs, full prompts, full chunks, or embedding arrays. Safe operational metadata such as document ids, conversation ids, counts, and status transitions may be logged when needed.

## Delete Cleanup

Deleting a document first checks ownership. For the owner, deletion removes:

- the local encrypted upload file
- the document's `document_chunks`
- embedding data stored on those chunks
- the `documents` row

Historical chat messages are not deleted by module eight. Their sources remain safe snapshots and must not include file paths or user ids.

## Not Implemented In Module 8

The following are intentionally deferred:

- database field-level encryption for chunks, extracted text, and embeddings
- a full audit log subsystem
- advanced RBAC or shared-document permissions
- key rotation and multi-key decrypt support
- per-user encryption keys
- prompt redaction or PII detection
- encrypted vector search
