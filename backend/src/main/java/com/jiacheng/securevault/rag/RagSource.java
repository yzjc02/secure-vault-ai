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
    private final String embeddingModel;
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
                     String embeddingModel,
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
        this.embeddingModel = embeddingModel;
        this.embeddedAt = embeddedAt;
    }

    public static RagSource from(SimilarChunkResponse chunk, int index, int previewLength) {
        String content = chunk.getContent();
        return new RagSource(
                "S" + index,
                chunk.getChunkId(),
                chunk.getDocumentId(),
                chunk.getDocumentTitle(),
                chunk.getOriginalFilename(),
                chunk.getChunkIndex(),
                chunk.getScore(),
                content,
                truncate(content, previewLength),
                chunk.getEmbeddingModel(),
                chunk.getEmbeddedAt()
        );
    }

    private static String truncate(String value, int maxCodePoints) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.codePointCount(0, value.length()) <= maxCodePoints) {
            return value;
        }
        int endIndex = value.offsetByCodePoints(0, maxCodePoints);
        return value.substring(0, endIndex);
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
                embeddingModel,
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
    public String getEmbeddingModel() { return embeddingModel; }
    public LocalDateTime getEmbeddedAt() { return embeddedAt; }
}
