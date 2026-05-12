package com.jiacheng.securevault.document.dto;

import java.time.LocalDateTime;

public class SimilarChunkResponse {

    private Long chunkId;
    private Long documentId;
    private String documentTitle;
    private String originalFilename;
    private Integer chunkIndex;
    private Double score;
    private String content;
    private String contentPreview;
    private LocalDateTime embeddedAt;

    public SimilarChunkResponse() {
    }

    public SimilarChunkResponse(Long chunkId,
                                Long documentId,
                                String documentTitle,
                                String originalFilename,
                                Integer chunkIndex,
                                Double score,
                                String content,
                                LocalDateTime embeddedAt) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.originalFilename = originalFilename;
        this.chunkIndex = chunkIndex;
        this.score = safeScore(score);
        this.content = content;
        this.contentPreview = preview(content);
        this.embeddedAt = embeddedAt;
    }

    private static Double safeScore(Double score) {
        if (score == null || score.isNaN() || score.isInfinite()) {
            return 0.0d;
        }
        return Math.max(0.0d, Math.min(1.0d, score));
    }

    private static String preview(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        int maxCodePoints = 160;
        if (content.codePointCount(0, content.length()) <= maxCodePoints) {
            return content;
        }
        int endIndex = content.offsetByCodePoints(0, maxCodePoints);
        return content.substring(0, endIndex);
    }

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
    public void setScore(Double score) { this.score = safeScore(score); }
    public String content() { return content; }
    public String getContentPreview() { return contentPreview; }
    public void setContentPreview(String contentPreview) { this.contentPreview = contentPreview; }
    public LocalDateTime getEmbeddedAt() { return embeddedAt; }
    public void setEmbeddedAt(LocalDateTime embeddedAt) { this.embeddedAt = embeddedAt; }
}
