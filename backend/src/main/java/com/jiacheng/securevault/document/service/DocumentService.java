package com.jiacheng.securevault.document.service;

import com.jiacheng.securevault.document.dto.DocumentCreateRequest;
import com.jiacheng.securevault.document.dto.DocumentResponse;
import com.jiacheng.securevault.document.dto.DocumentUpdateRequest;
import com.jiacheng.securevault.document.entity.Document;
import com.jiacheng.securevault.document.repository.DocumentRepository;
import com.jiacheng.securevault.exception.BusinessException;
import com.jiacheng.securevault.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class DocumentService {

    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    private final DocumentRepository documentRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;

    public DocumentService(DocumentRepository documentRepository,
                           CurrentUserService currentUserService,
                           FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public DocumentResponse create(DocumentCreateRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        String title = normalizeTitle(request.getTitle());
        String description = normalizeDescription(request.getDescription());

        Document document = new Document();
        document.setUserId(currentUserId);
        document.setTitle(title);
        document.setDescription(description);
        document.setStatus(Document.STATUS_CREATED);

        return toResponse(documentRepository.save(document));
    }

    @Transactional
    public DocumentResponse upload(MultipartFile file, String title) {
        Long currentUserId = currentUserService.getCurrentUserId();
        FileStorageService.StoredFile storedFile = fileStorageService.store(file);

        Document document = new Document();
        document.setUserId(currentUserId);
        document.setTitle(normalizeUploadTitle(title, storedFile.originalFilename()));
        document.setDescription(null);
        document.setStatus(Document.STATUS_UPLOADED);
        document.setOriginalFilename(storedFile.originalFilename());
        document.setStoredFilename(storedFile.storedFilename());
        document.setFilePath(storedFile.filePath());
        document.setFileType(storedFile.fileType());
        document.setFileSize(storedFile.fileSize());
        document.setContentType(storedFile.contentType());

        try {
            return toResponse(documentRepository.save(document));
        } catch (RuntimeException ex) {
            fileStorageService.delete(storedFile.storedFilename());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listCurrentUserDocuments() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return documentRepository.findAllByUserIdOrderByCreatedAtDesc(currentUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(Long id) {
        Long currentUserId = currentUserService.getCurrentUserId();
        return toResponse(getOwnedDocument(id, currentUserId));
    }

    @Transactional
    public DocumentResponse update(Long id, DocumentUpdateRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Document document = getOwnedDocument(id, currentUserId);
        document.setTitle(normalizeTitle(request.getTitle()));
        document.setDescription(normalizeDescription(request.getDescription()));
        return toResponse(documentRepository.save(document));
    }

    @Transactional
    public void delete(Long id) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Document document = getOwnedDocument(id, currentUserId);
        fileStorageService.delete(document.getStoredFilename());
        documentRepository.delete(document);
    }

    private Document getOwnedDocument(Long id, Long currentUserId) {
        return documentRepository.findByIdAndUserId(id, currentUserId)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));
    }

    private String normalizeTitle(String title) {
        String normalized = title == null ? "" : title.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(400, "标题不能为空");
        }
        if (normalized.length() > MAX_TITLE_LENGTH) {
            throw new BusinessException(400, "标题长度不能超过120");
        }
        return normalized;
    }

    private String normalizeUploadTitle(String title, String originalFilename) {
        if (!StringUtils.hasText(title)) {
            return normalizeTitle(originalFilename);
        }
        return normalizeTitle(title);
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException(400, "描述长度不能超过1000");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getDescription(),
                document.getStatus(),
                document.getOriginalFilename(),
                document.getStoredFilename(),
                document.getFileType(),
                document.getFileSize(),
                document.getContentType(),
                document.getErrorMessage(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
