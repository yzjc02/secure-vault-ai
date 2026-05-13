package com.jiacheng.securevault.audit.dto;

import com.jiacheng.securevault.audit.entity.AuditLog;
import com.jiacheng.securevault.audit.enums.AuditAction;
import com.jiacheng.securevault.audit.enums.AuditResourceType;
import com.jiacheng.securevault.audit.support.AuditSanitizer;

import java.time.LocalDateTime;

public class AuditLogResponse {

    private Long id;
    private AuditAction action;
    private AuditResourceType resourceType;
    private Long resourceId;
    private boolean success;
    private String message;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;

    public AuditLogResponse() {
    }

    public AuditLogResponse(Long id,
                            AuditAction action,
                            AuditResourceType resourceType,
                            Long resourceId,
                            boolean success,
                            String message,
                            String ipAddress,
                            String userAgent,
                            LocalDateTime createdAt) {
        this.id = id;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.success = success;
        this.message = message;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
    }

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.isSuccess(),
                AuditSanitizer.sanitizeAndLimit(auditLog.getMessage(), 512),
                AuditSanitizer.sanitizeAndLimit(auditLog.getIpAddress(), 64),
                AuditSanitizer.sanitizeAndLimit(auditLog.getUserAgent(), 255),
                auditLog.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }
    public AuditResourceType getResourceType() { return resourceType; }
    public void setResourceType(AuditResourceType resourceType) { this.resourceType = resourceType; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
