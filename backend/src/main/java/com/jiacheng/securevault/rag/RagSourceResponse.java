package com.jiacheng.securevault.rag;

import java.time.LocalDateTime;

public class RagSourceResponse {

    private String sourceId;
    private Long chunkId;
    private Long documentId;
    private String documentTitle;
    private String originalFilename;
    private Integer chunkIndex;
    private Double score;
    private String contentPreview;
    private String snippet;
    private LocalDateTime embeddedAt;
    private LocalDateTime createdAt;

    public RagSourceResponse() {
    }

    public RagSourceResponse(String sourceId,
                             Long chunkId,
                             Long documentId,
                             String documentTitle,
                             String originalFilename,
                             Integer chunkIndex,
                             Double score,
                             String contentPreview,
                             LocalDateTime embeddedAt) {
        this(sourceId, chunkId, documentId, documentTitle, originalFilename, chunkIndex, score,
                contentPreview, contentPreview, embeddedAt, embeddedAt);
    }

    public RagSourceResponse(String sourceId,
                             Long chunkId,
                             Long documentId,
                             String documentTitle,
                             String originalFilename,
                             Integer chunkIndex,
                             Double score,
                             String contentPreview,
                             String snippet,
                             LocalDateTime embeddedAt,
                             LocalDateTime createdAt) {
        this.sourceId = sourceId;
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.originalFilename = originalFilename;
        this.chunkIndex = chunkIndex;
        this.score = score;
        this.contentPreview = contentPreview;
        this.snippet = snippet == null ? "" : snippet;
        this.embeddedAt = embeddedAt;
        this.createdAt = createdAt;
    }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public Long getChunkId() { return chunkId; }
    public void setChunkId(Long chunkId) { this.chunkId = chunkId; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getContentPreview() { return contentPreview; }
    public void setContentPreview(String contentPreview) { this.contentPreview = contentPreview; }
    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet == null ? "" : snippet; }
    public LocalDateTime getEmbeddedAt() { return embeddedAt; }
    public void setEmbeddedAt(LocalDateTime embeddedAt) { this.embeddedAt = embeddedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
