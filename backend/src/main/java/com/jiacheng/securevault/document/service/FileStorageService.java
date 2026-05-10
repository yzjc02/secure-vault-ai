package com.jiacheng.securevault.document.service;

import com.jiacheng.securevault.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private static final String UNSUPPORTED_FILE_TYPE_MESSAGE =
            "不支持的文件类型，仅支持 pdf、docx、txt、md、markdown";

    private final FileStorageProperties properties;
    private Path uploadRoot;
    private Set<String> allowedExtensions;

    public FileStorageService(FileStorageProperties properties) {
        this.properties = properties;
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
            throw new IllegalStateException("无法创建文件上传目录: " + uploadRoot, ex);
        }
    }

    public StoredFile store(MultipartFile file) {
        validateFile(file);

        String originalFilename = extractFilename(file.getOriginalFilename());
        String extension = extractAllowedExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path destination = resolveInsideUploadRoot(storedFilename);

        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException(500, "文件保存失败");
        }

        return new StoredFile(
                originalFilename,
                storedFilename,
                destination.toString(),
                extension,
                file.getSize(),
                file.getContentType()
        );
    }

    public void delete(String storedFilename) {
        if (!StringUtils.hasText(storedFilename)) {
            return;
        }

        Path target = resolveInsideUploadRoot(storedFilename);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new BusinessException(500, "文件删除失败");
        }
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }

    public Path resolveStoredFile(String storedFilename, String filePath) {
        Path target = resolveInsideUploadRoot(storedFilename);

        if (!StringUtils.hasText(filePath)) {
            throw new BusinessException(400, "文件路径非法");
        }

        Path recordedPath = Paths.get(filePath).toAbsolutePath().normalize();
        if (!recordedPath.startsWith(uploadRoot)) {
            throw new BusinessException(400, "文件路径非法");
        }
        if (!target.getFileName().equals(recordedPath.getFileName())) {
            throw new BusinessException(400, "文件路径非法");
        }
        return target;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        if (file.getSize() > properties.getMaxFileSize()) {
            throw new BusinessException(400, "文件大小不能超过 " + formatMaxFileSize(properties.getMaxFileSize()));
        }
    }

    private String extractFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException(400, "上传文件名不能为空");
        }

        String normalizedSeparators = originalFilename.replace('\\', '/');
        String filename = normalizedSeparators.substring(normalizedSeparators.lastIndexOf('/') + 1).trim();
        if (!StringUtils.hasText(filename)) {
            throw new BusinessException(400, "上传文件名不能为空");
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
            throw new BusinessException(400, "文件名非法");
        }

        Path target = uploadRoot.resolve(filename).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BusinessException(400, "文件路径非法");
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
                             String contentType) {
    }
}
