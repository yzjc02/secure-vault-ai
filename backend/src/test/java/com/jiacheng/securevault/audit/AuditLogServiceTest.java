package com.jiacheng.securevault.audit;

import com.jiacheng.securevault.audit.entity.AuditLog;
import com.jiacheng.securevault.audit.enums.AuditAction;
import com.jiacheng.securevault.audit.enums.AuditResourceType;
import com.jiacheng.securevault.audit.repository.AuditLogRepository;
import com.jiacheng.securevault.audit.service.AuditLogService;
import com.jiacheng.securevault.security.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogServiceTest {

    @Test
    void shouldWriteSanitizedAndTruncatedAuditLog() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ObjectProvider<HttpServletRequest> requestProvider = mockRequestProvider("127.0.0.1", "A".repeat(300));
        AuditLogService service = new AuditLogService(repository, currentUserService, requestProvider, new NoOpTransactionManager());

        when(repository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String message = "password=secret filePath=C:\\temp\\secret.txt " + "x".repeat(700);

        service.recordForUser(42L, AuditAction.DOCUMENT_UPLOAD_SUCCESS, AuditResourceType.DOCUMENT, 9L, true, message);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(42L);
        assertThat(saved.getAction()).isEqualTo(AuditAction.DOCUMENT_UPLOAD_SUCCESS);
        assertThat(saved.getResourceType()).isEqualTo(AuditResourceType.DOCUMENT);
        assertThat(saved.getResourceId()).isEqualTo(9L);
        assertThat(saved.isSuccess()).isTrue();
        assertThat(saved.getMessage()).hasSizeLessThanOrEqualTo(512);
        assertThat(saved.getMessage()).doesNotContain("password").doesNotContain("C:\\");
        assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(saved.getUserAgent()).hasSize(255);
    }

    @Test
    void shouldNotThrowWhenRepositoryWriteFails() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ObjectProvider<HttpServletRequest> requestProvider = mock(ObjectProvider.class);
        AuditLogService service = new AuditLogService(repository, currentUserService, requestProvider, new NoOpTransactionManager());

        when(repository.save(any(AuditLog.class))).thenThrow(new IllegalStateException("database unavailable"));

        service.recordAnonymous(AuditAction.LOGIN_FAILURE, AuditResourceType.AUTH, null, false, "Login failed");
    }

    private ObjectProvider<HttpServletRequest> mockRequestProvider(String remoteAddr, String userAgent) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn(userAgent);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        ObjectProvider<HttpServletRequest> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(request);
        return provider;
    }

    private static class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
