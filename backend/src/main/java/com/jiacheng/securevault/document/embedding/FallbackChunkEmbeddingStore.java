package com.jiacheng.securevault.document.embedding;

import com.jiacheng.securevault.document.dto.SimilarChunkResponse;
import com.jiacheng.securevault.document.entity.Document;
import com.jiacheng.securevault.document.entity.DocumentChunk;
import com.jiacheng.securevault.document.repository.DocumentChunkRepository;
import com.jiacheng.securevault.document.repository.DocumentRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FallbackChunkEmbeddingStore implements ChunkEmbeddingStore {

    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;

    public FallbackChunkEmbeddingStore(DocumentChunkRepository documentChunkRepository,
                                       DocumentRepository documentRepository) {
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
    }

    @Override
    public void saveEmbedding(Long userId, Long chunkId, List<Double> embedding, String model, int dimension) {
        DocumentChunk chunk = documentChunkRepository.findByIdAndUserId(chunkId, userId)
                .orElseThrow(() -> new EmbeddingException("Embedding chunk update failed"));
        chunk.setEmbeddingJson(EmbeddingVectorUtils.toJson(embedding, dimension));
        chunk.setEmbeddingModel(model);
        chunk.setEmbeddingDimension(dimension);
        chunk.setEmbeddedAt(LocalDateTime.now());
        documentChunkRepository.save(chunk);
    }

    @Override
    public int countEmbeddedChunks(Long userId, Long documentId) {
        return (int) documentChunkRepository.countByUserIdAndDocumentIdAndEmbeddingJsonIsNotNull(userId, documentId);
    }

    @Override
    public List<SimilarChunkResponse> search(Long userId,
                                             String queryVectorString,
                                             List<Double> queryEmbedding,
                                             Long documentId,
                                             int topK) {
        List<DocumentChunk> chunks = documentId == null
                ? documentChunkRepository.findAllByUserIdAndEmbeddingJsonIsNotNull(userId)
                : documentChunkRepository.findAllByUserIdAndDocumentIdAndEmbeddingJsonIsNotNullOrderByChunkIndexAsc(userId, documentId);
        Map<Long, Document> documents = documentRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .collect(Collectors.toMap(Document::getId, Function.identity()));
        return chunks.stream()
                .map(chunk -> toResponse(chunk, documents.get(chunk.getDocumentId()), queryEmbedding))
                .sorted(Comparator.comparing(SimilarChunkResponse::getScore).reversed()
                        .thenComparing(SimilarChunkResponse::getChunkId))
                .limit(topK)
                .toList();
    }

    private SimilarChunkResponse toResponse(DocumentChunk chunk, Document document, List<Double> queryEmbedding) {
        List<Double> embedding = EmbeddingVectorUtils.parseJson(chunk.getEmbeddingJson());
        double score = EmbeddingVectorUtils.cosineSimilarity(queryEmbedding, embedding);
        return new SimilarChunkResponse(
                chunk.getId(),
                chunk.getDocumentId(),
                document == null ? null : document.getTitle(),
                document == null ? null : document.getOriginalFilename(),
                chunk.getChunkIndex(),
                score,
                chunk.getContent(),
                chunk.getEmbeddedAt()
        );
    }
}
