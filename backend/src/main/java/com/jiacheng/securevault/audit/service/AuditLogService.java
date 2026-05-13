package com.jiacheng.securevault.audit.service;

import com.jiacheng.securevault.audit.dto.AuditLogPageResponse;
import com.jiacheng.securevault.audit.dto.AuditLogResponse;
import com.jiacheng.securevault.audit.entity.AuditLog;
import com.jiacheng.securevault.audit.enums.AuditAction;
import com.jiacheng.securevault.audit.enums.AuditResourceType;
import com.jiacheng.securevault.audit.repository.AuditLogRepository;
import com.jiacheng.securevault.audit.support.AuditSanitizer;
import com.jiacheng.securevault.exception.BusinessException;
import com.jiacheng.securevault.security.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;
    private final ObjectProvider<HttpServletRequest> requestProvider;
    private final TransactionTemplate transactionTemplate;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           CurrentUserService currentUserService,
                           ObjectProvider<HttpServletRequest> requestProvider,
                           PlatformTransactionManager transactionManager) {
        this.auditLogRepository = auditLogRepository;
        this.currentUserService = currentUserService;
        this.requestProvider = requestProvider;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void recordForCurrentUser(AuditAction action,
                                     AuditResourceType resourceType,
                                     Long resourceId,
                                     boolean success,
                                     String message) {
        try {
            recordForUser(currentUserService.getCurrentUserId(), action, resourceType, resourceId, success, message);
        } catch (Exception ex) {
            log.warn("Audit current user lookup failed");
        }
    }

    public void recordForUser(Long userId,
                              AuditAction action,
                              AuditResourceType resourceType,
                              Long resourceId,
                              boolean success,
                              String message) {
        HttpServletRequest request = currentRequest();
        safeRecord(userId, action, resourceType, resourceId, success, message,
                extractIpAddress(request), extractUserAgent(request));
    }

    public void recordAnonymous(AuditAction action,
                                AuditResourceType resourceType,
                                Long resourceId,
                                boolean success,
                                String message) {
        HttpServletRequest request = currentRequest();
        safeRecord(null, action, resourceType, resourceId, success, message,
                extractIpAddress(request), extractUserAgent(request));
    }

    public AuditLogPageResponse listCurrentUserLogs(int page,
                                                    int size,
                                                    AuditAction action,
                                                    AuditResourceType resourceType,
                                                    Boolean success) {
        Long currentUserId = currentUserService.getCurrentUserId();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<AuditLog> logs = auditLogRepository.findCurrentUserLogs(currentUserId, action, resourceType, success, pageable);
        recordForUser(currentUserId, AuditAction.AUDIT_LOG_READ, AuditResourceType.AUDIT_LOG, null, true, "Audit logs read");
        return new AuditLogPageResponse(
                logs.getContent().stream().map(AuditLogResponse::from).toList(),
                logs.getNumber(),
                logs.getSize(),
                logs.getTotalElements(),
                logs.getTotalPages()
        );
    }

    public AuditLogResponse getCurrentUserLog(Long id) {
        Long currentUserId = currentUserService.getCurrentUserId();
        AuditLog auditLog = auditLogRepository.findByIdAndUserId(id, currentUserId)
                .orElseThrow(() -> new BusinessException(404, "Audit log not found"));
        recordForUser(currentUserId, AuditAction.AUDIT_LOG_READ, AuditResourceType.AUDIT_LOG, id, true, "Audit log read");
        return AuditLogResponse.from(auditLog);
    }

    private void safeRecord(Long userId,
                            AuditAction action,
                            AuditResourceType resourceType,
                            Long resourceId,
                            boolean success,
                            String message,
                            String ipAddress,
                            String userAgent) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                AuditLog auditLog = new AuditLog();
                auditLog.setUserId(userId);
                auditLog.setAction(action);
                auditLog.setResourceType(resourceType);
                auditLog.setResourceId(resourceId);
                auditLog.setSuccess(success);
                auditLog.setMessage(AuditSanitizer.sanitizeAndLimit(message, 512));
                auditLog.setIpAddress(AuditSanitizer.sanitizeAndLimit(ipAddress, 64));
                auditLog.setUserAgent(AuditSanitizer.sanitizeAndLimit(userAgent, 255));
                auditLogRepository.save(auditLog);
            });
        } catch (Exception ex) {
            log.warn("Audit log write failed: {}", ex.getClass().getSimpleName());
        }
    }

    private HttpServletRequest currentRequest() {
        try {
            return requestProvider.getIfAvailable();
        } catch (Exception ex) {
            return null;
        }
    }

    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        return request == null ? null : request.getHeader("User-Agent");
    }
}
