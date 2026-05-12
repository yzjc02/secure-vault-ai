package com.jiacheng.securevault.document.service;

import com.jiacheng.securevault.document.dto.DocumentResponse;
import com.jiacheng.securevault.document.dto.EmbeddingStatusResponse;
import com.jiacheng.securevault.document.dto.SemanticSearchRequest;
import com.jiacheng.securevault.document.dto.SimilarChunkResponse;
import com.jiacheng.securevault.document.embedding.ChunkEmbeddingStore;
import com.jiacheng.securevault.document.embedding.EmbeddingClient;
import com.jiacheng.securevault.document.embedding.EmbeddingException;
import com.jiacheng.securevault.document.embedding.EmbeddingProperties;
import com.jiacheng.securevault.document.embedding.EmbeddingVectorUtils;
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
public class DocumentEmbeddingService {

    private static final String DOCUMENT_NOT_FOUND = "Document not found";
    private static final String DOCUMENT_HAS_NO_CHUNKS = "Document has no chunks";
    private static final String EMBEDDING_FAILED = "Embedding model request failed";

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final CurrentUserService currentUserService;
    private final EmbeddingClient embeddingClient;
    private final ChunkEmbeddingStore chunkEmbeddingStore;
    private final EmbeddingProperties embeddingProperties;

    public DocumentEmbeddingService(DocumentRepository documentRepository,
                                    DocumentChunkRepository documentChunkRepository,
                                    CurrentUserService currentUserService,
                                    EmbeddingClient embeddingClient,
                                    ChunkEmbeddingStore chunkEmbeddingStore,
                                    EmbeddingProperties embeddingProperties) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.currentUserService = currentUserService;
        this.embeddingClient = embeddingClient;
        this.chunkEmbeddingStore = chunkEmbeddingStore;
        this.embeddingProperties = embeddingProperties;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public DocumentResponse embedDocument(Long documentId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Document document = getOwnedDocument(documentId, currentUserId);
        List<DocumentChunk> chunks = documentChunkRepository.findAllByUserIdAndDocumentIdOrderByChunkIndexAsc(currentUserId, documentId);
        if (chunks.isEmpty()) {
            throw new BusinessException(400, DOCUMENT_HAS_NO_CHUNKS);
        }

        document.setStatus(Document.STATUS_EMBEDDING);
        document.setErrorMessage(null);
        documentRepository.save(document);

        try {
            for (DocumentChunk chunk : chunks) {
                List<Double> embedding = embeddingClient.embed(chunk.getContent());
                EmbeddingVectorUtils.validate(embedding, embeddingClient.dimension());
                chunkEmbeddingStore.saveEmbedding(currentUserId, chunk.getId(), embedding, embeddingClient.model(), embeddingClient.dimension());
            }
            int embeddedCount = chunkEmbeddingStore.countEmbeddedChunks(currentUserId, documentId);
            document.setStatus(Document.STATUS_EMBEDDED);
            document.setEmbeddedChunkCount(embeddedCount);
            document.setEmbeddedAt(LocalDateTime.now());
            document.setEmbeddingModel(embeddingClient.model());
            document.setEmbeddingDimension(embeddingClient.dimension());
            document.setErrorMessage(null);
            return DocumentResponse.from(documentRepository.save(document), true);
        } catch (BusinessException ex) {
            markEmbeddingFailed(document, ex.getMessage());
            throw ex;
        } catch (EmbeddingException ex) {
            markEmbeddingFailed(document, ex.getMessage());
            throw new BusinessException(400, safeErrorMessage(ex.getMessage()));
        } catch (RuntimeException ex) {
            markEmbeddingFailed(document, EMBEDDING_FAILED);
            throw new BusinessException(400, EMBEDDING_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public EmbeddingStatusResponse getEmbeddingStatus(Long documentId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Document document = getOwnedDocument(documentId, currentUserId);
        int chunkCount = (int) documentChunkRepository.countByUserIdAndDocumentId(currentUserId, documentId);
        int embeddedChunkCount = chunkEmbeddingStore.countEmbeddedChunks(currentUserId, documentId);
        return EmbeddingStatusResponse.from(document, chunkCount, embeddedChunkCount);
    }

    @Transactional(readOnly = true)
    public List<SimilarChunkResponse> searchSimilarChunks(SemanticSearchRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        String query = request == null ? null : request.getQuery();
        if (!StringUtils.hasText(query)) {
            throw new BusinessException(400, "query must not be blank");
        }
        Long documentId = request.getDocumentId();
        if (documentId != null) {
            getOwnedDocument(documentId, currentUserId);
        }
        int topK = request.getTopK() == null ? embeddingProperties.getTopK() : request.getTopK();
        if (topK < 1 || topK > 20) {
            throw new BusinessException(400, "topK must be between 1 and 20");
        }
        List<Double> queryEmbedding = embeddingClient.embed(query.trim());
        EmbeddingVectorUtils.validate(queryEmbedding, embeddingClient.dimension());
        String queryVectorString = EmbeddingVectorUtils.toPgvectorString(queryEmbedding, embeddingClient.dimension());
        return chunkEmbeddingStore.search(currentUserId, queryVectorString, queryEmbedding, documentId, topK);
    }

    private Document getOwnedDocument(Long documentId, Long currentUserId) {
        return documentRepository.findByIdAndUserId(documentId, currentUserId)
                .orElseThrow(() -> new BusinessException(404, DOCUMENT_NOT_FOUND));
    }

    private void markEmbeddingFailed(Document document, String message) {
        document.setStatus(Document.STATUS_FAILED);
        document.setErrorMessage(safeErrorMessage(message));
        documentRepository.save(document);
    }

    private String safeErrorMessage(String message) {
        String safeMessage = StringUtils.hasText(message) ? message : EMBEDDING_FAILED;
        safeMessage = safeMessage.replace('\r', ' ').replace('\n', ' ').trim();
        if (safeMessage.length() > 300) {
            return safeMessage.substring(0, 300);
        }
        return safeMessage;
    }
}
