package com.jiacheng.securevault.document.dto;

import com.jiacheng.securevault.document.entity.DocumentChunk;

import java.time.LocalDateTime;

public class DocumentChunkResponse {

    private Long id;
    private Long documentId;
    private Integer chunkIndex;
    private String content;
    private Integer contentLength;
    private Integer tokenCount;
    private String contentHash;
    private Integer startOffset;
    private Integer endOffset;
    private LocalDateTime createdAt;

    public DocumentChunkResponse() {
    }

    public DocumentChunkResponse(Long id,
                                 Long documentId,
                                 Integer chunkIndex,
                                 String content,
                                 Integer contentLength,
                                 Integer tokenCount,
                                 String contentHash,
                                 Integer startOffset,
                                 Integer endOffset,
                                 LocalDateTime createdAt) {
        this.id = id;
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.contentLength = contentLength;
        this.tokenCount = tokenCount;
        this.contentHash = contentHash;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.createdAt = createdAt;
    }

    public static DocumentChunkResponse from(DocumentChunk chunk) {
        return new DocumentChunkResponse(
                chunk.getId(),
                chunk.getDocumentId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                chunk.getContentLength(),
                chunk.getTokenCount(),
                chunk.getContentHash(),
                chunk.getStartOffset(),
                chunk.getEndOffset(),
                chunk.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getContentLength() { return contentLength; }
    public void setContentLength(Integer contentLength) { this.contentLength = contentLength; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public Integer getStartOffset() { return startOffset; }
    public void setStartOffset(Integer startOffset) { this.startOffset = startOffset; }
    public Integer getEndOffset() { return endOffset; }
    public void setEndOffset(Integer endOffset) { this.endOffset = endOffset; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
