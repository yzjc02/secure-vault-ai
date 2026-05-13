package com.jiacheng.securevault.rag;

import com.jiacheng.securevault.document.dto.SimilarChunkResponse;

import java.time.LocalDateTime;

public class RagSource {

    private final String sourceId;
    private final Long chunkId;
    private final Long documentId;
    private final String documentTitle;
    private final String originalFilename;
    private final Integer chunkIndex;
    private final Double score;
    private final String content;
    private final String contentPreview;
    private final LocalDateTime embeddedAt;

    public RagSource(String sourceId,
                     Long chunkId,
                     Long documentId,
                     String documentTitle,
                     String originalFilename,
                     Integer chunkIndex,
                     Double score,
                     String content,
                     String contentPreview,
                     LocalDateTime embeddedAt) {
        this.sourceId = sourceId;
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.originalFilename = originalFilename;
        this.chunkIndex = chunkIndex;
        this.score = score;
        this.content = content;
        this.contentPreview = contentPreview;
        this.embeddedAt = embeddedAt;
    }

    public static RagSource from(SimilarChunkResponse chunk, int index, int previewLength) {
        String content = chunk.content();
        return new RagSource(
                "S" + index,
                chunk.getChunkId(),
                chunk.getDocumentId(),
                chunk.getDocumentTitle(),
                chunk.getOriginalFilename(),
                chunk.getChunkIndex(),
                chunk.getScore(),
                content,
                preview(content, previewLength),
                chunk.getEmbeddedAt()
        );
    }

    private static String preview(String value, int maxCodePoints) {
        if (value == null || value.isBlank()) {
            return "[No preview available]";
        }
        String sanitized = sanitizePreview(value);
        if (sanitized.isBlank()) {
            return "[No preview available]";
        }
        if (sanitized.codePointCount(0, sanitized.length()) <= maxCodePoints) {
            return sanitized;
        }
        int endIndex = sanitized.offsetByCodePoints(0, maxCodePoints);
        return sanitized.substring(0, endIndex) + "...";
    }

    private static String sanitizePreview(String value) {
        return value.replaceAll("\\s+", " ")
                .replaceAll("(?i)[A-Z]:\\\\\\S+", "[local path removed]")
                .replaceAll("(?i)/\\S*/uploads/\\S*", "[local path removed]")
                .trim();
    }

    public RagSourceResponse toResponse() {
        return new RagSourceResponse(
                sourceId,
                chunkId,
                documentId,
                documentTitle,
                originalFilename,
                chunkIndex,
                score,
                contentPreview,
                embeddedAt
        );
    }

    public String getSourceId() { return sourceId; }
    public Long getChunkId() { return chunkId; }
    public Long getDocumentId() { return documentId; }
    public String getDocumentTitle() { return documentTitle; }
    public String getOriginalFilename() { return originalFilename; }
    public Integer getChunkIndex() { return chunkIndex; }
    public Double getScore() { return score; }
    public String getContent() { return content; }
    public String getContentPreview() { return contentPreview; }
    public LocalDateTime getEmbeddedAt() { return embeddedAt; }
}
