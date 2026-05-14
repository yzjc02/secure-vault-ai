package com.jiacheng.securevault.document.service;

import com.jiacheng.securevault.audit.enums.AuditAction;
import com.jiacheng.securevault.audit.enums.AuditResourceType;
import com.jiacheng.securevault.audit.service.AuditLogService;
import com.jiacheng.securevault.document.dto.DocumentReindexResponse;
import com.jiacheng.securevault.document.embedding.ChunkEmbeddingStore;
import com.jiacheng.securevault.document.embedding.EmbeddingClient;
import com.jiacheng.securevault.document.embedding.EmbeddingVectorUtils;
import com.jiacheng.securevault.document.entity.Document;
import com.jiacheng.securevault.document.entity.DocumentChunk;
import com.jiacheng.securevault.document.repository.DocumentChunkRepository;
import com.jiacheng.securevault.document.repository.DocumentRepository;
import com.jiacheng.securevault.exception.BusinessException;
import com.jiacheng.securevault.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentReindexService {

    private static final String DOCUMENT_NOT_FOUND = "Document not found";
    private static final String DOCUMENT_NOT_PARSED = "文档尚未解析，无法重新索引";
    private static final String DOCUMENT_REINDEXING = "文档正在重新索引，请勿重复提交";
    private static final String DOCUMENT_REINDEX_FAILED = "文档重新索引失败";

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final CurrentUserService currentUserService;
    private final TextChunkingService textChunkingService;
    private final EmbeddingClient embeddingClient;
    private final ChunkEmbeddingStore chunkEmbeddingStore;
    private final AuditLogService auditLogService;
    private final TransactionTemplate transactionTemplate;

    public DocumentReindexService(DocumentRepository documentRepository,
                                  DocumentChunkRepository documentChunkRepository,
                                  CurrentUserService currentUserService,
                                  TextChunkingService textChunkingService,
                                  EmbeddingClient embeddingClient,
                                  ChunkEmbeddingStore chunkEmbeddingStore,
                                  AuditLogService auditLogService,
                                  PlatformTransactionManager transactionManager) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.currentUserService = currentUserService;
        this.textChunkingService = textChunkingService;
        this.embeddingClient = embeddingClient;
        this.chunkEmbeddingStore = chunkEmbeddingStore;
        this.auditLogService = auditLogService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public DocumentReindexResponse reindexDocument(Long documentId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        ReindexContext context = new ReindexContext();

        try {
            context.oldStatus = prepareReindex(currentUserId, documentId);
            context.started = true;
            ReindexResult result = executeReindex(currentUserId, documentId);
            context.deletedChunkCount = result.deletedChunkCount();
            context.newChunkCount = result.response().getChunkCount();
            context.embeddedChunkCount = result.response().getEmbeddedChunkCount();
            recordSuccess(currentUserId, documentId, context);
            return result.response();
        } catch (BusinessException ex) {
            if (context.started) {
                markReindexFailed(currentUserId, documentId, ex.getMessage());
            }
            recordFailure(currentUserId, documentId, context, ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            if (context.started) {
                markReindexFailed(currentUserId, documentId, DOCUMENT_REINDEX_FAILED);
            }
            recordFailure(currentUserId, documentId, context, DOCUMENT_REINDEX_FAILED);
            throw new BusinessException(500, DOCUMENT_REINDEX_FAILED);
        }
    }

    private String prepareReindex(Long currentUserId, Long documentId) {
        return transactionTemplate.execute(status -> {
            Document document = documentRepository.findByIdAndUserIdForUpdate(documentId, currentUserId)
                    .orElseThrow(() -> new BusinessException(404, DOCUMENT_NOT_FOUND));
            String oldStatus = document.getStatus();
            if (Document.STATUS_REINDEXING.equals(oldStatus)) {
                throw new BusinessException(400, DOCUMENT_REINDEXING);
            }
            if (!StringUtils.hasText(document.getExtractedText())) {
                throw new BusinessException(400, DOCUMENT_NOT_PARSED);
            }

            document.setStatus(Document.STATUS_REINDEXING);
            document.setErrorMessage(null);
            documentRepository.saveAndFlush(document);
            return oldStatus;
        });
    }

    private ReindexResult executeReindex(Long currentUserId, Long documentId) {
        return transactionTemplate.execute(status -> {
            Document document = documentRepository.findByIdAndUserIdForUpdate(documentId, currentUserId)
                    .orElseThrow(() -> new BusinessException(404, DOCUMENT_NOT_FOUND));
            int deletedChunkCount = documentChunkRepository
                    .deleteByUserIdAndDocumentIdDirectly(currentUserId, documentId);
            resetIndexMetadata(document);

            List<TextChunkingService.ChunkCandidate> candidates = textChunkingService.split(document.getExtractedText());
            if (candidates.isEmpty()) {
                throw new BusinessException(400, DOCUMENT_NOT_PARSED);
            }

            List<DocumentChunk> chunks = candidates.stream()
                    .map(candidate -> toChunk(document, candidate))
                    .toList();
            List<DocumentChunk> savedChunks = documentChunkRepository.saveAll(chunks);
            documentChunkRepository.flush();

            for (DocumentChunk chunk : savedChunks) {
                List<Double> embedding = embeddingClient.embed(chunk.getContent());
                EmbeddingVectorUtils.validate(embedding, embeddingClient.dimension());
                chunkEmbeddingStore.saveEmbedding(currentUserId, chunk.getId(), embedding,
                        embeddingClient.model(), embeddingClient.dimension());
            }

            int embeddedChunkCount = chunkEmbeddingStore.countEmbeddedChunks(currentUserId, documentId);
            LocalDateTime now = LocalDateTime.now();
            document.setStatus(Document.STATUS_EMBEDDED);
            document.setChunkCount(savedChunks.size());
            document.setChunkedAt(now);
            document.setEmbeddedChunkCount(embeddedChunkCount);
            document.setEmbeddedAt(now);
            document.setEmbeddingModel(embeddingClient.model());
            document.setEmbeddingDimension(embeddingClient.dimension());
            document.setErrorMessage(null);
            Document savedDocument = documentRepository.save(document);
            return new ReindexResult(
                    DocumentReindexResponse.from(savedDocument, savedChunks.size(), embeddedChunkCount, now),
                    deletedChunkCount
            );
        });
    }

    private void markReindexFailed(Long currentUserId, Long documentId, String message) {
        transactionTemplate.executeWithoutResult(status -> documentRepository.findByIdAndUserIdForUpdate(documentId, currentUserId)
                .ifPresent(document -> {
                    document.setStatus(Document.STATUS_REINDEX_FAILED);
                    document.setErrorMessage(safeErrorMessage(message));
                    documentRepository.save(document);
                }));
    }

    private void resetIndexMetadata(Document document) {
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

    private void recordSuccess(Long currentUserId, Long documentId, ReindexContext context) {
        auditLogService.recordForUser(currentUserId, AuditAction.DOCUMENT_REINDEX,
                AuditResourceType.DOCUMENT, documentId, true,
                "Document reindex success oldStatus=" + safeStatus(context.oldStatus)
                        + " newStatus=" + Document.STATUS_EMBEDDED
                        + " deletedChunkCount=" + context.deletedChunkCount
                        + " newChunkCount=" + context.newChunkCount
                        + " embeddedChunkCount=" + context.embeddedChunkCount);
    }

    private void recordFailure(Long currentUserId, Long documentId, ReindexContext context, String message) {
        auditLogService.recordForUser(currentUserId, AuditAction.DOCUMENT_REINDEX,
                AuditResourceType.DOCUMENT, documentId, false,
                "Document reindex failed oldStatus=" + safeStatus(context.oldStatus)
                        + " newStatus=" + (context.started ? Document.STATUS_REINDEX_FAILED : safeStatus(context.oldStatus))
                        + " deletedChunkCount=" + context.deletedChunkCount
                        + " newChunkCount=" + context.newChunkCount
                        + " embeddedChunkCount=" + context.embeddedChunkCount
                        + " error=" + safeErrorMessage(message));
    }

    private String safeStatus(String status) {
        return StringUtils.hasText(status) ? status : "UNKNOWN";
    }

    private String safeErrorMessage(String message) {
        String safeMessage = StringUtils.hasText(message) ? message : DOCUMENT_REINDEX_FAILED;
        safeMessage = safeMessage.replace('\r', ' ').replace('\n', ' ').trim();
        if (safeMessage.length() > 300) {
            return safeMessage.substring(0, 300);
        }
        return safeMessage;
    }

    private static class ReindexContext {
        private boolean started;
        private String oldStatus;
        private int deletedChunkCount;
        private int newChunkCount;
        private int embeddedChunkCount;

        private ReindexContext() {
        }
    }

    private record ReindexResult(DocumentReindexResponse response, int deletedChunkCount) {
    }
}
