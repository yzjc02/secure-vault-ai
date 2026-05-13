package com.jiacheng.securevault.audit;

import com.jiacheng.securevault.audit.support.AuditSanitizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditSanitizerTest {

    @Test
    void shouldRedactSensitiveAuditMessageContent() {
        String sanitized = AuditSanitizer.sanitize("""
                Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSJ9.signature
                password=secret rawPassword=secret
                JWT_SECRET=secret FILE_ENCRYPTION_KEY=secret encryptionKey=secret
                filePath=C:\\Users\\alice\\secure-vault-ai\\uploads\\doc.txt
                storedFilename=stored.txt fullPrompt=prompt embedding=[0.001, -0.002, 0.003, 0.004]
                jdbc:postgresql://localhost:5432/securevault
                /uploads/abc.txt .secure-vault/file-encryption.key
                at com.example.Service.method(Service.java:1)
                """);

        assertThat(sanitized)
                .contains(AuditSanitizer.REDACTED)
                .doesNotContain("Bearer ")
                .doesNotContain("eyJhbGci")
                .doesNotContain("password")
                .doesNotContain("rawPassword")
                .doesNotContain("JWT_SECRET")
                .doesNotContain("FILE_ENCRYPTION_KEY")
                .doesNotContain("encryptionKey")
                .doesNotContain("filePath")
                .doesNotContain("storedFilename")
                .doesNotContain("fullPrompt")
                .doesNotContain("embedding")
                .doesNotContain("jdbc:")
                .doesNotContain("C:\\")
                .doesNotContain("/uploads/")
                .doesNotContain(".secure-vault")
                .doesNotContain("file-encryption.key")
                .doesNotContain("Service.java");
    }
}
