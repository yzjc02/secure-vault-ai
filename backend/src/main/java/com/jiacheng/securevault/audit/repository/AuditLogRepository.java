package com.jiacheng.securevault.audit.repository;

import com.jiacheng.securevault.audit.entity.AuditLog;
import com.jiacheng.securevault.audit.enums.AuditAction;
import com.jiacheng.securevault.audit.enums.AuditResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where a.userId = :userId
              and (:action is null or a.action = :action)
              and (:resourceType is null or a.resourceType = :resourceType)
              and (:success is null or a.success = :success)
            """)
    Page<AuditLog> findCurrentUserLogs(@Param("userId") Long userId,
                                       @Param("action") AuditAction action,
                                       @Param("resourceType") AuditResourceType resourceType,
                                       @Param("success") Boolean success,
                                       Pageable pageable);

    Optional<AuditLog> findByIdAndUserId(Long id, Long userId);
}
