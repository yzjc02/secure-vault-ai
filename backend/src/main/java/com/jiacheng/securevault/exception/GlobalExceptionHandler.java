package com.jiacheng.securevault.exception;

import com.jiacheng.securevault.common.ApiResponse;
import com.jiacheng.securevault.config.EncodingConfig;
import com.jiacheng.securevault.document.service.FileStorageProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final FileStorageProperties fileStorageProperties;

    public GlobalExceptionHandler(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(EncodingConfig.APPLICATION_JSON_UTF8)
                .body(ApiResponse.fail(400, message));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(resolveStatus(ex.getCode()))
                .contentType(EncodingConfig.APPLICATION_JSON_UTF8)
                .body(ApiResponse.fail(ex.getCode(), safeMessage(ex.getMessage())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(EncodingConfig.APPLICATION_JSON_UTF8)
                .body(ApiResponse.fail(400, "请求体格式错误"));
    }

    @ExceptionHandler({
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestPartException(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(EncodingConfig.APPLICATION_JSON_UTF8)
                .body(ApiResponse.fail(400, "请求参数不完整"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(EncodingConfig.APPLICATION_JSON_UTF8)
                .body(ApiResponse.fail(400, "文件大小不能超过 " + formatMaxFileSize(fileStorageProperties.getMaxFileSize())));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipartException(MultipartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(EncodingConfig.APPLICATION_JSON_UTF8)
                .body(ApiResponse.fail(400, "文件上传请求格式错误"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(EncodingConfig.APPLICATION_JSON_UTF8)
                .body(ApiResponse.fail(401, "Unauthorized"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(EncodingConfig.APPLICATION_JSON_UTF8)
                .body(ApiResponse.fail(403, "Forbidden"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(EncodingConfig.APPLICATION_JSON_UTF8)
                .body(ApiResponse.fail(500, "服务器内部错误"));
    }

    private HttpStatus resolveStatus(int code) {
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 404 -> HttpStatus.NOT_FOUND;
            case 403 -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private String formatMaxFileSize(long maxFileSize) {
        long mb = 1024L * 1024L;
        if (maxFileSize % mb == 0) {
            return (maxFileSize / mb) + "MB";
        }
        return maxFileSize + "B";
    }

    private String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Request failed";
        }
        String safe = message.replace('\r', ' ').replace('\n', ' ').trim();
        if (safe.contains("jdbc:") || safe.contains("Bearer ") || safe.contains("FILE_ENCRYPTION_KEY")) {
            return "Request failed";
        }
        if (safe.contains("\\") || safe.contains("/uploads/") || safe.contains("/app/data/uploads")) {
            return "Request failed";
        }
        return safe.length() > 300 ? safe.substring(0, 300) : safe;
    }
}
