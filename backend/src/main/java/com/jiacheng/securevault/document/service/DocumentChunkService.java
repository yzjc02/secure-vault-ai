package com.jiacheng.securevault.document.service;

import com.jiacheng.securevault.document.dto.DocumentChunkResponse;
import com.jiacheng.securevault.document.dto.DocumentResponse;
import com.jiacheng.securevault.document.entity.Document;
import com.jiacheng.securevault.document.entity.DocumentChunk;
import com.jiacheng.securevault.document.repository.DocumentChunkRepository;
import com.jiacheng.securevault.document.repository.DocumentRepository;
import com.jiacheng.securevault.exception.BusinessException;
import com.jiacheng.securevault.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentChunkService {

    private static final String DOCUMENT_NOT_FOUND = "文档不存在";
    private static final String DOCUMENT_NOT_PARSED = "文档尚未解析，无法分块";
    private static final String DOCUMENT_PARSING = "文档正在解析，暂不能分块";
    private static final String DOCUMENT_CHUNKING_FAILED = "文档分块失败";

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final CurrentUserService currentUserService;
    private final TextChunkingService textChunkingService;

    public DocumentChunkService(DocumentRepository documentRepository,
                                DocumentChunkRepository documentChunkRepository,
                                CurrentUserService currentUserService,
                                TextChunkingService textChunkingService) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.currentUserService = currentUserService;
        this.textChunkingService = textChunkingService;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public DocumentResponse chunkDocument(Long documentId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        return chunkForUser(documentId, currentUserId);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public DocumentResponse chunkForUser(Long documentId, Long currentUserId) {
        Document document = getOwnedDocument(documentId, currentUserId);
        validateChunkable(document);

        document.setStatus(Document.STATUS_CHUNKING);
        document.setErrorMessage(null);
        documentRepository.save(document);
        clearChunks(document);

        try {
            List<TextChunkingService.ChunkCandidate> candidates = textChunkingService.split(document.getExtractedText());
            if (candidates.isEmpty()) {
                throw new BusinessException(400, DOCUMENT_NOT_PARSED);
            }

            List<DocumentChunk> chunks = candidates.stream()
                    .map(candidate -> toChunk(document, candidate))
                    .toList();
            documentChunkRepository.saveAll(chunks);

            document.setStatus(Document.STATUS_CHUNKED);
            document.setChunkCount(chunks.size());
            document.setChunkedAt(LocalDateTime.now());
            document.setErrorMessage(null);
            return DocumentResponse.from(documentRepository.save(document), true);
        } catch (BusinessException ex) {
            markChunkingFailed(document, ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            markChunkingFailed(document, DOCUMENT_CHUNKING_FAILED);
            throw new BusinessException(500, DOCUMENT_CHUNKING_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentChunkResponse> listChunks(Long documentId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        getOwnedDocument(documentId, currentUserId);
        return documentChunkRepository.findAllByUserIdAndDocumentIdOrderByChunkIndexAsc(currentUserId, documentId)
                .stream()
                .map(DocumentChunkResponse::from)
                .toList();
    }

    @Transactional
    public void clearChunksForDocument(Document document) {
        clearChunks(document);
        document.setChunkCount(0);
        document.setChunkedAt(null);
        documentRepository.save(document);
    }

    private Document getOwnedDocument(Long documentId, Long currentUserId) {
        return documentRepository.findByIdAndUserId(documentId, currentUserId)
                .orElseThrow(() -> new BusinessException(404, DOCUMENT_NOT_FOUND));
    }

    private void validateChunkable(Document document) {
        if (Document.STATUS_PARSING.equals(document.getStatus())) {
            throw new BusinessException(400, DOCUMENT_PARSING);
        }
        if (!StringUtils.hasText(document.getExtractedText())) {
            throw new BusinessException(400, DOCUMENT_NOT_PARSED);
        }
    }

    private void clearChunks(Document document) {
        documentChunkRepository.deleteByUserIdAndDocumentId(document.getUserId(), document.getId());
        document.setChunkCount(0);
        document.setChunkedAt(null);
        document.setEmbeddedChunkCount(0);
        document.setEmbeddedAt(null);
        document.setEmbeddingModel(null);
        document.setEmbeddingDimension(null);
    }

    private DocumentChunk toChunk(Document document, TextChunkingService.ChunkCandidate candidate) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setUserId(document.getUserId());
        chunk.setDocumentId(document.getId());
        chunk.setChunkIndex(candidate.chunkIndex());
        chunk.setContent(candidate.content());
        chunk.setContentLength(candidate.contentLength());
        chunk.setTokenCount(candidate.tokenCount());
        chunk.setContentHash(candidate.contentHash());
        chunk.setStartOffset(candidate.startOffset());
        chunk.setEndOffset(candidate.endOffset());
        return chunk;
    }

    private void markChunkingFailed(Document document, String message) {
        clearChunks(document);
        document.setStatus(Document.STATUS_FAILED);
        document.setChunkCount(0);
        document.setChunkedAt(null);
        document.setErrorMessage(safeErrorMessage(message));
        documentRepository.save(document);
    }

    private String safeErrorMessage(String message) {
        String safeMessage = StringUtils.hasText(message) ? message : DOCUMENT_CHUNKING_FAILED;
        safeMessage = safeMessage.replace('\r', ' ').replace('\n', ' ').trim();
        if (safeMessage.length() > 300) {
            return safeMessage.substring(0, 300);
        }
        return safeMessage;
    }
}
