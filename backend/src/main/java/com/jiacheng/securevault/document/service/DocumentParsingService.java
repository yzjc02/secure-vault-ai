package com.jiacheng.securevault.document.service;

import com.jiacheng.securevault.audit.enums.AuditAction;
import com.jiacheng.securevault.audit.enums.AuditResourceType;
import com.jiacheng.securevault.audit.service.AuditLogService;
import com.jiacheng.securevault.document.dto.DocumentResponse;
import com.jiacheng.securevault.document.dto.DocumentTextResponse;
import com.jiacheng.securevault.document.entity.Document;
import com.jiacheng.securevault.document.parser.DocumentParseException;
import com.jiacheng.securevault.document.parser.DocumentTextParser;
import com.jiacheng.securevault.document.repository.DocumentRepository;
import com.jiacheng.securevault.exception.BusinessException;
import com.jiacheng.securevault.security.AccessControlService;
import com.jiacheng.securevault.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.time.LocalDateTime;

@Service
public class DocumentParsingService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 300;

    private final DocumentRepository documentRepository;
    private final CurrentUserService currentUserService;
    private final AccessControlService accessControlService;
    private final FileStorageService fileStorageService;
    private final DocumentTextParser documentTextParser;
    private final DocumentChunkService documentChunkService;
    private final AuditLogService auditLogService;

    public DocumentParsingService(DocumentRepository documentRepository,
                                  CurrentUserService currentUserService,
                                  AccessControlService accessControlService,
                                  FileStorageService fileStorageService,
                                  DocumentTextParser documentTextParser,
                                  DocumentChunkService documentChunkService,
                                  AuditLogService auditLogService) {
        this.documentRepository = documentRepository;
        this.currentUserService = currentUserService;
        this.accessControlService = accessControlService;
        this.fileStorageService = fileStorageService;
        this.documentTextParser = documentTextParser;
        this.documentChunkService = documentChunkService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public DocumentResponse parse(Long documentId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        return parseForUser(documentId, currentUserId);
    }

    @Transactional
    public DocumentResponse parseForUser(Long documentId, Long currentUserId) {
        Document document = accessControlService.requireOwnedDocument(documentId, currentUserId);
        if (!hasUploadedFile(document)) {
            throw new BusinessException(400, "Document has no uploaded file to parse");
        }

        document.setStatus(Document.STATUS_PARSING);
        document.setErrorMessage(null);
        document.setParsedAt(null);
        documentChunkService.clearChunksForDocument(document);
        documentRepository.save(document);

        try {
            InputStream plaintextStream = fileStorageService.openInputStream(
                    document.getStoredFilename(),
                    document.getFilePath()
            );
            String extractedText = documentTextParser.parse(plaintextStream, document.getFileType(), document.getContentType());
            document.setExtractedText(extractedText);
            document.setTextLength(extractedText.length());
            document.setParsedAt(LocalDateTime.now());
            document.setStatus(Document.STATUS_PARSED);
            document.setErrorMessage(null);
            Document savedDocument = documentRepository.save(document);
            DocumentResponse response = documentChunkService.chunkForUser(savedDocument.getId(), currentUserId);
            auditLogService.recordForUser(currentUserId, AuditAction.DOCUMENT_PARSE_SUCCESS,
                    AuditResourceType.DOCUMENT, documentId, true, "Document parsed");
            return response;
        } catch (DocumentParseException ex) {
            return markFailed(document, currentUserId, ex.getMessage());
        } catch (BusinessException ex) {
            return markFailed(document, currentUserId, ex.getMessage());
        } catch (RuntimeException ex) {
            return markFailed(document, currentUserId, "Document text parse failed");
        }
    }

    @Transactional(readOnly = true)
    public DocumentTextResponse getText(Long documentId) {
        Document document = accessControlService.requireOwnedDocument(documentId);
        boolean parsedStatus = Document.STATUS_PARSED.equals(document.getStatus())
                || Document.STATUS_CHUNKED.equals(document.getStatus())
                || Document.STATUS_EMBEDDED.equals(document.getStatus());
        if (!parsedStatus || !StringUtils.hasText(document.getExtractedText())) {
            throw new BusinessException(400, "Document has not been parsed successfully");
        }
        return DocumentTextResponse.from(document);
    }

    private DocumentResponse markFailed(Document document, Long currentUserId, String message) {
        document.setStatus(Document.STATUS_FAILED);
        document.setExtractedText(null);
        document.setTextLength(0);
        document.setParsedAt(LocalDateTime.now());
        documentChunkService.clearChunksForDocument(document);
        document.setErrorMessage(safeErrorMessage(message));
        auditLogService.recordForUser(currentUserId, AuditAction.DOCUMENT_PARSE_FAILURE,
                AuditResourceType.DOCUMENT, document.getId(), false, "Document parse failed");
        return DocumentResponse.from(documentRepository.save(document), true);
    }

    private boolean hasUploadedFile(Document document) {
        return StringUtils.hasText(document.getStoredFilename()) && StringUtils.hasText(document.getFilePath());
    }

    private String safeErrorMessage(String message) {
        String safeMessage = StringUtils.hasText(message) ? message : "Document text parse failed";
        safeMessage = safeMessage.replace('\r', ' ').replace('\n', ' ').trim();
        if (safeMessage.length() > MAX_ERROR_MESSAGE_LENGTH) {
            return safeMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
        }
        return safeMessage;
    }
}
