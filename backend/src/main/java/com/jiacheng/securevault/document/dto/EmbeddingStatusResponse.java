package com.jiacheng.securevault.document.dto;

import com.jiacheng.securevault.document.entity.Document;

import java.time.LocalDateTime;

public class EmbeddingStatusResponse {

    private Long documentId;
    private String status;
    private Integer chunkCount;
    private Integer embeddedChunkCount;
    private LocalDateTime embeddedAt;
    private LocalDateTime updatedAt;
    private String errorMessage;

    public EmbeddingStatusResponse() {
    }

    public EmbeddingStatusResponse(Long documentId,
                                   String status,
                                   Integer chunkCount,
                                   Integer embeddedChunkCount,
                                   LocalDateTime embeddedAt,
                                   LocalDateTime updatedAt,
                                   String errorMessage) {
        this.documentId = documentId;
        this.status = status;
        this.chunkCount = chunkCount;
        this.embeddedChunkCount = embeddedChunkCount;
        this.embeddedAt = embeddedAt;
        this.updatedAt = updatedAt;
        this.errorMessage = errorMessage;
    }

    public static EmbeddingStatusResponse from(Document document, int chunkCount, int embeddedChunkCount) {
        return new EmbeddingStatusResponse(
                document.getId(),
                document.getStatus(),
                chunkCount,
                embeddedChunkCount,
                document.getEmbeddedAt(),
                document.getUpdatedAt(),
                document.getErrorMessage()
        );
    }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public Integer getEmbeddedChunkCount() { return embeddedChunkCount; }
    public void setEmbeddedChunkCount(Integer embeddedChunkCount) { this.embeddedChunkCount = embeddedChunkCount; }
    public LocalDateTime getEmbeddedAt() { return embeddedAt; }
    public void setEmbeddedAt(LocalDateTime embeddedAt) { this.embeddedAt = embeddedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
