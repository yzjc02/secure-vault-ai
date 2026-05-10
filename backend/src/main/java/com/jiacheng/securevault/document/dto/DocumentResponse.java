package com.jiacheng.securevault.document.dto;

import com.jiacheng.securevault.document.entity.Document;

import java.time.LocalDateTime;

public class DocumentResponse {

    private Long id;
    private String title;
    private String description;
    private String status;
    private String originalFilename;
    private String storedFilename;
    private String fileType;
    private Long fileSize;
    private String contentType;
    private String errorMessage;
    private Integer textLength;
    private LocalDateTime parsedAt;
    private String extractedTextPreview;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DocumentResponse() {
    }

    public DocumentResponse(Long id,
                            String title,
                            String description,
                            String status,
                            String originalFilename,
                            String storedFilename,
                            String fileType,
                            Long fileSize,
                            String contentType,
                            String errorMessage,
                            Integer textLength,
                            LocalDateTime parsedAt,
                            String extractedTextPreview,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.errorMessage = errorMessage;
        this.textLength = textLength;
        this.parsedAt = parsedAt;
        this.extractedTextPreview = extractedTextPreview;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DocumentResponse from(Document document, boolean includeTextPreview) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getDescription(),
                document.getStatus(),
                document.getOriginalFilename(),
                document.getStoredFilename(),
                document.getFileType(),
                document.getFileSize(),
                document.getContentType(),
                document.getErrorMessage(),
                document.getTextLength(),
                document.getParsedAt(),
                includeTextPreview ? preview(document.getExtractedText()) : null,
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    private static String preview(String extractedText) {
        if (extractedText == null || extractedText.isBlank()) {
            return null;
        }
        int previewLength = Math.min(extractedText.length(), 300);
        return extractedText.substring(0, previewLength);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Integer getTextLength() { return textLength; }
    public void setTextLength(Integer textLength) { this.textLength = textLength; }
    public LocalDateTime getParsedAt() { return parsedAt; }
    public void setParsedAt(LocalDateTime parsedAt) { this.parsedAt = parsedAt; }
    public String getExtractedTextPreview() { return extractedTextPreview; }
    public void setExtractedTextPreview(String extractedTextPreview) { this.extractedTextPreview = extractedTextPreview; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
