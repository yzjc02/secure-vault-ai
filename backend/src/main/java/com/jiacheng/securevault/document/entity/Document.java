package com.jiacheng.securevault.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_user_id", columnList = "user_id")
})
public class Document {

    public static final String STATUS_CREATED = "CREATED";
    public static final String STATUS_UPLOADED = "UPLOADED";
    public static final String STATUS_PARSING = "PARSING";
    public static final String STATUS_PARSED = "PARSED";
    public static final String STATUS_CHUNKING = "CHUNKING";
    public static final String STATUS_CHUNKED = "CHUNKED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", length = 80)
    private String storedFilename;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_type", length = 32)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Lob
    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "text_length")
    private Integer textLength;

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;

    @Column(name = "chunk_count")
    private Integer chunkCount = 0;

    @Column(name = "chunked_at")
    private LocalDateTime chunkedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = STATUS_CREATED;
        }
        if (this.chunkCount == null) {
            this.chunkCount = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
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
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
    public Integer getTextLength() { return textLength; }
    public void setTextLength(Integer textLength) { this.textLength = textLength; }
    public LocalDateTime getParsedAt() { return parsedAt; }
    public void setParsedAt(LocalDateTime parsedAt) { this.parsedAt = parsedAt; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public LocalDateTime getChunkedAt() { return chunkedAt; }
    public void setChunkedAt(LocalDateTime chunkedAt) { this.chunkedAt = chunkedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
