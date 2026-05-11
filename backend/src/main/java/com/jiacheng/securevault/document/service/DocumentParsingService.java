package com.jiacheng.securevault.document.service;

import com.jiacheng.securevault.document.dto.DocumentResponse;
import com.jiacheng.securevault.document.dto.DocumentTextResponse;
import com.jiacheng.securevault.document.entity.Document;
import com.jiacheng.securevault.document.parser.DocumentParseException;
import com.jiacheng.securevault.document.parser.DocumentTextParser;
import com.jiacheng.securevault.document.repository.DocumentRepository;
import com.jiacheng.securevault.exception.BusinessException;
import com.jiacheng.securevault.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
public class DocumentParsingService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 300;

    private final DocumentRepository documentRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final DocumentTextParser documentTextParser;
    private final DocumentChunkService documentChunkService;

    public DocumentParsingService(DocumentRepository documentRepository,
                                  CurrentUserService currentUserService,
                                  FileStorageService fileStorageService,
                                  DocumentTextParser documentTextParser,
                                  DocumentChunkService documentChunkService) {
        this.documentRepository = documentRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.documentTextParser = documentTextParser;
        this.documentChunkService = documentChunkService;
    }

    @Transactional
    public DocumentResponse parse(Long documentId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        return parseForUser(documentId, currentUserId);
    }

    @Transactional
    public DocumentResponse parseForUser(Long documentId, Long currentUserId) {
        Document document = getOwnedDocument(documentId, currentUserId);
        if (!hasUploadedFile(document)) {
            throw new BusinessException(400, "该文档没有可解析的上传文件");
        }

        document.setStatus(Document.STATUS_PARSING);
        document.setErrorMessage(null);
        document.setParsedAt(null);
        documentChunkService.clearChunksForDocument(document);
        documentRepository.save(document);

        Path storedPath;
        try {
            storedPath = fileStorageService.resolveStoredFile(document.getStoredFilename(), document.getFilePath());
        } catch (BusinessException ex) {
            return markFailed(document, ex.getMessage());
        }

        if (!Files.isRegularFile(storedPath)) {
            return markFailed(document, "本地文件不存在");
        }

        try {
            String extractedText = documentTextParser.parse(storedPath, document.getFileType(), document.getContentType());
            document.setExtractedText(extractedText);
            document.setTextLength(extractedText.length());
            document.setParsedAt(LocalDateTime.now());
            document.setStatus(Document.STATUS_PARSED);
            document.setErrorMessage(null);
            Document savedDocument = documentRepository.save(document);
            return documentChunkService.chunkForUser(savedDocument.getId(), currentUserId);
        } catch (DocumentParseException ex) {
            return markFailed(document, ex.getMessage());
        } catch (BusinessException ex) {
            return markFailed(document, ex.getMessage());
        } catch (RuntimeException ex) {
            return markFailed(document, "文档文本解析失败");
        }
    }

    @Transactional(readOnly = true)
    public DocumentTextResponse getText(Long documentId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Document document = getOwnedDocument(documentId, currentUserId);
        boolean parsedStatus = Document.STATUS_PARSED.equals(document.getStatus()) || Document.STATUS_CHUNKED.equals(document.getStatus());
        if (!parsedStatus || !StringUtils.hasText(document.getExtractedText())) {
            throw new BusinessException(400, "文档尚未解析成功");
        }
        return DocumentTextResponse.from(document);
    }

    private DocumentResponse markFailed(Document document, String message) {
        document.setStatus(Document.STATUS_FAILED);
        document.setExtractedText(null);
        document.setTextLength(0);
        document.setParsedAt(LocalDateTime.now());
        documentChunkService.clearChunksForDocument(document);
        document.setErrorMessage(safeErrorMessage(message));
        return DocumentResponse.from(documentRepository.save(document), true);
    }

    private Document getOwnedDocument(Long documentId, Long currentUserId) {
        return documentRepository.findByIdAndUserId(documentId, currentUserId)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));
    }

    private boolean hasUploadedFile(Document document) {
        return StringUtils.hasText(document.getStoredFilename()) && StringUtils.hasText(document.getFilePath());
    }

    private String safeErrorMessage(String message) {
        String safeMessage = StringUtils.hasText(message) ? message : "文档文本解析失败";
        safeMessage = safeMessage.replace('\r', ' ').replace('\n', ' ').trim();
        if (safeMessage.length() > MAX_ERROR_MESSAGE_LENGTH) {
            return safeMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
        }
        return safeMessage;
    }
}
