package com.jiacheng.securevault.document.dto;

import com.jiacheng.securevault.document.entity.Document;

import java.time.LocalDateTime;

public class DocumentTextResponse {

    private Long id;
    private String title;
    private String status;
    private String originalFilename;
    private String fileType;
    private Integer textLength;
    private LocalDateTime parsedAt;
    private String extractedText;

    public DocumentTextResponse() {
    }

    public DocumentTextResponse(Long id,
                                String title,
                                String status,
                                String originalFilename,
                                String fileType,
                                Integer textLength,
                                LocalDateTime parsedAt,
                                String extractedText) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.originalFilename = originalFilename;
        this.fileType = fileType;
        this.textLength = textLength;
        this.parsedAt = parsedAt;
        this.extractedText = extractedText;
    }

    public static DocumentTextResponse from(Document document) {
        return new DocumentTextResponse(
                document.getId(),
                document.getTitle(),
                document.getStatus(),
                document.getOriginalFilename(),
                document.getFileType(),
                document.getTextLength(),
                document.getParsedAt(),
                document.getExtractedText()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Integer getTextLength() { return textLength; }
    public void setTextLength(Integer textLength) { this.textLength = textLength; }
    public LocalDateTime getParsedAt() { return parsedAt; }
    public void setParsedAt(LocalDateTime parsedAt) { this.parsedAt = parsedAt; }
    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
}
