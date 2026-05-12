package com.jiacheng.securevault.document.repository;

import com.jiacheng.securevault.document.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findAllByUserIdAndDocumentIdOrderByChunkIndexAsc(Long userId, Long documentId);

    List<DocumentChunk> findAllByUserIdAndEmbeddingJsonIsNotNull(Long userId);

    List<DocumentChunk> findAllByUserIdAndDocumentIdAndEmbeddingJsonIsNotNullOrderByChunkIndexAsc(Long userId, Long documentId);

    Optional<DocumentChunk> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndDocumentId(Long userId, Long documentId);

    long countByUserIdAndDocumentIdAndEmbeddingJsonIsNotNull(Long userId, Long documentId);

    void deleteByUserIdAndDocumentId(Long userId, Long documentId);
}
