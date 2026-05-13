package com.jiacheng.securevault.audit.support;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

public final class AuditSanitizer {

    public static final String REDACTED = "[REDACTED]";

    private static final List<Pattern> SENSITIVE_PATTERNS = List.of(
            Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._~+\\-/=]+"),
            Pattern.compile("\\b[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\b"),
            Pattern.compile("(?i)(password|rawPassword|FILE_ENCRYPTION_KEY|JWT_SECRET|encryptionKey|fullPrompt|embedding|filePath|storedFilename)\\s*[:=]\\s*[^\\s,;}]+"),
            Pattern.compile("(?i)\\b(password|rawPassword|FILE_ENCRYPTION_KEY|JWT_SECRET|encryptionKey|fullPrompt|embedding|filePath|storedFilename)\\b"),
            Pattern.compile("(?i)jdbc:[^\\s,;}]+"),
            Pattern.compile("(?i)[A-Z]:\\\\[^\\s,;}]+"),
            Pattern.compile("(?i)/uploads/[^\\s,;}]*"),
            Pattern.compile("(?i)\\.secure-vault[^\\s,;}]*"),
            Pattern.compile("(?i)file-encryption\\.key"),
            Pattern.compile("\\[(?:\\s*-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\s*,){2,}[^\\]]*\\]")
    );

    private AuditSanitizer() {
    }

    public static String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String sanitized = removeStackTraceLines(value);
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll(REDACTED);
        }
        sanitized = sanitized.replace('\r', ' ').replace('\n', ' ').trim();
        return sanitized.isBlank() ? null : sanitized;
    }

    public static String sanitizeAndLimit(String value, int maxLength) {
        String sanitized = sanitize(value);
        if (sanitized == null) {
            return null;
        }
        if (sanitized.length() > maxLength) {
            return sanitized.substring(0, maxLength);
        }
        return sanitized;
    }

    private static String removeStackTraceLines(String value) {
        StringBuilder builder = new StringBuilder();
        for (String line : value.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("at ") || trimmed.startsWith("Caused by:") || trimmed.startsWith("Suppressed:")) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(trimmed);
        }
        return builder.toString();
    }
}
