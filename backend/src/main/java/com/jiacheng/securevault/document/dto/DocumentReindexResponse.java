package com.jiacheng.securevault.document.dto;

import com.jiacheng.securevault.document.entity.Document;

import java.time.LocalDateTime;

public class DocumentReindexResponse {

    private Long documentId;
    private String title;
    private String status;
    private Integer chunkCount;
    private Integer embeddedChunkCount;
    private LocalDateTime reindexedAt;

    public DocumentReindexResponse() {
    }

    public DocumentReindexResponse(Long documentId,
                                   String title,
                                   String status,
                                   Integer chunkCount,
                                   Integer embeddedChunkCount,
                                   LocalDateTime reindexedAt) {
        this.documentId = documentId;
        this.title = title;
        this.status = status;
        this.chunkCount = chunkCount;
        this.embeddedChunkCount = embeddedChunkCount;
        this.reindexedAt = reindexedAt;
    }

    public static DocumentReindexResponse from(Document document,
                                               int chunkCount,
                                               int embeddedChunkCount,
                                               LocalDateTime reindexedAt) {
        return new DocumentReindexResponse(
                document.getId(),
                document.getTitle(),
                document.getStatus(),
                chunkCount,
                embeddedChunkCount,
                reindexedAt
        );
    }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public Integer getEmbeddedChunkCount() { return embeddedChunkCount; }
    public void setEmbeddedChunkCount(Integer embeddedChunkCount) { this.embeddedChunkCount = embeddedChunkCount; }
    public LocalDateTime getReindexedAt() { return reindexedAt; }
    public void setReindexedAt(LocalDateTime reindexedAt) { this.reindexedAt = reindexedAt; }
}
