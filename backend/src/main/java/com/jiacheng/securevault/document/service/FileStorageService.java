package com.jiacheng.securevault.document.service;

import com.jiacheng.securevault.exception.BusinessException;
import com.jiacheng.securevault.security.encryption.FileEncryptionService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private static final String UNSUPPORTED_FILE_TYPE_MESSAGE =
            "Unsupported file type. Only pdf, docx, txt, md, markdown are allowed";

    private final FileStorageProperties properties;
    private final FileEncryptionService fileEncryptionService;
    private Path uploadRoot;
    private Set<String> allowedExtensions;

    public FileStorageService(FileStorageProperties properties,
                              FileEncryptionService fileEncryptionService) {
        this.properties = properties;
        this.fileEncryptionService = fileEncryptionService;
    }

    @PostConstruct
    public void init() {
        this.uploadRoot = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        this.allowedExtensions = properties.getAllowedExtensions()
                .stream()
                .map(extension -> extension.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create upload directory", ex);
        }
    }

    public StoredFile store(MultipartFile file) {
        validateFile(file);

        String originalFilename = extractFilename(file.getOriginalFilename());
        String extension = extractAllowedExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path destination = resolveInsideUploadRoot(storedFilename);

        try {
            byte[] plaintext = file.getBytes();
            Files.write(destination, fileEncryptionService.encrypt(plaintext));
        } catch (IOException ex) {
            throw new BusinessException(500, "File save failed");
        }

        boolean encrypted = fileEncryptionService.isEnabled();
        return new StoredFile(
                originalFilename,
                storedFilename,
                destination.toString(),
                extension,
                file.getSize(),
                file.getContentType(),
                encrypted,
                encrypted ? FileEncryptionService.ALGORITHM : null,
                null
        );
    }

    public byte[] readFileBytes(String storedFilename, String filePath) {
        Path target = resolveStoredFile(storedFilename, filePath);
        if (!Files.isRegularFile(target)) {
            throw new BusinessException(404, "File not found");
        }
        try {
            return fileEncryptionService.decrypt(Files.readAllBytes(target));
        } catch (IOException ex) {
            throw new BusinessException(500, "File read failed");
        }
    }

    public InputStream openInputStream(String storedFilename, String filePath) {
        return new ByteArrayInputStream(readFileBytes(storedFilename, filePath));
    }

    public void delete(String storedFilename) {
        if (!StringUtils.hasText(storedFilename)) {
            return;
        }

        Path target = resolveInsideUploadRoot(storedFilename);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new BusinessException(500, "File delete failed");
        }
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }

    public Path resolveStoredFile(String storedFilename, String filePath) {
        Path target = resolveInsideUploadRoot(storedFilename);

        if (!StringUtils.hasText(filePath)) {
            throw new BusinessException(400, "Invalid file path");
        }

        Path recordedPath = Paths.get(filePath).toAbsolutePath().normalize();
        if (!recordedPath.startsWith(uploadRoot)) {
            throw new BusinessException(400, "Invalid file path");
        }
        if (!target.getFileName().equals(recordedPath.getFileName())) {
            throw new BusinessException(400, "Invalid file path");
        }
        return target;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "Upload file must not be empty");
        }
        if (file.getSize() > properties.getMaxFileSize()) {
            throw new BusinessException(400, "File size must not exceed " + formatMaxFileSize(properties.getMaxFileSize()));
        }
    }

    private String extractFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException(400, "Upload filename must not be blank");
        }

        String normalizedSeparators = originalFilename.replace('\\', '/');
        String filename = normalizedSeparators.substring(normalizedSeparators.lastIndexOf('/') + 1).trim();
        if (!StringUtils.hasText(filename)) {
            throw new BusinessException(400, "Upload filename must not be blank");
        }
        return filename;
    }

    private String extractAllowedExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == filename.length() - 1) {
            throw new BusinessException(400, UNSUPPORTED_FILE_TYPE_MESSAGE);
        }

        String extension = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!allowedExtensions.contains(extension)) {
            throw new BusinessException(400, UNSUPPORTED_FILE_TYPE_MESSAGE);
        }
        return extension;
    }

    private Path resolveInsideUploadRoot(String storedFilename) {
        String filename = extractFilename(storedFilename);
        if (!filename.equals(storedFilename)) {
            throw new BusinessException(400, "Invalid filename");
        }

        Path target = uploadRoot.resolve(filename).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BusinessException(400, "Invalid file path");
        }
        return target;
    }

    private String formatMaxFileSize(long maxFileSize) {
        long mb = 1024L * 1024L;
        if (maxFileSize % mb == 0) {
            return (maxFileSize / mb) + "MB";
        }
        return maxFileSize + "B";
    }

    public record StoredFile(String originalFilename,
                             String storedFilename,
                             String filePath,
                             String fileType,
                             long fileSize,
                             String contentType,
                             boolean encrypted,
                             String encryptionAlgorithm,
                             String encryptionKeyId) {
    }
}
