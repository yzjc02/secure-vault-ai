package com.jiacheng.securevault.document.dto;

import com.jiacheng.securevault.document.entity.Document;
import com.jiacheng.securevault.document.entity.DocumentChunk;

import java.time.LocalDateTime;

public class DocumentChunkDetailResponse {

    private Long documentId;
    private String documentTitle;
    private String originalFilename;
    private Integer chunkIndex;
    private String content;
    private Integer textLength;
    private LocalDateTime createdAt;

    public DocumentChunkDetailResponse() {
    }

    public DocumentChunkDetailResponse(Long documentId,
                                       String documentTitle,
                                       String originalFilename,
                                       Integer chunkIndex,
                                       String content,
                                       Integer textLength,
                                       LocalDateTime createdAt) {
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.originalFilename = originalFilename;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.textLength = textLength;
        this.createdAt = createdAt;
    }

    public static DocumentChunkDetailResponse from(Document document, DocumentChunk chunk) {
        String content = chunk.getContent() == null ? "" : chunk.getContent();
        return new DocumentChunkDetailResponse(
                document.getId(),
                document.getTitle(),
                document.getOriginalFilename(),
                chunk.getChunkIndex(),
                content,
                chunk.getContentLength() == null ? content.length() : chunk.getContentLength(),
                chunk.getCreatedAt() == null ? document.getCreatedAt() : chunk.getCreatedAt()
        );
    }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getTextLength() { return textLength; }
    public void setTextLength(Integer textLength) { this.textLength = textLength; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
