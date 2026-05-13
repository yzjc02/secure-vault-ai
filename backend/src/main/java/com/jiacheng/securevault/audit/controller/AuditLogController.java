package com.jiacheng.securevault.audit.controller;

import com.jiacheng.securevault.audit.dto.AuditLogPageResponse;
import com.jiacheng.securevault.audit.dto.AuditLogResponse;
import com.jiacheng.securevault.audit.enums.AuditAction;
import com.jiacheng.securevault.audit.enums.AuditResourceType;
import com.jiacheng.securevault.audit.service.AuditLogService;
import com.jiacheng.securevault.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<AuditLogPageResponse> list(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(required = false) AuditAction action,
                                                  @RequestParam(required = false) AuditResourceType resourceType,
                                                  @RequestParam(required = false) Boolean success) {
        return ApiResponse.success(auditLogService.listCurrentUserLogs(page, size, action, resourceType, success));
    }

    @GetMapping("/{id}")
    public ApiResponse<AuditLogResponse> get(@PathVariable Long id) {
        return ApiResponse.success(auditLogService.getCurrentUserLog(id));
    }
}
