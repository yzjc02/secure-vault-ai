package com.jiacheng.securevault.document.repository;

import com.jiacheng.securevault.document.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findAllByUserIdAndDocumentIdOrderByChunkIndexAsc(Long userId, Long documentId);

    List<DocumentChunk> findAllByUserIdAndEmbeddingJsonIsNotNull(Long userId);

    List<DocumentChunk> findAllByUserIdAndDocumentIdAndEmbeddingJsonIsNotNullOrderByChunkIndexAsc(Long userId, Long documentId);

    Optional<DocumentChunk> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndDocumentId(Long userId, Long documentId);

    long countByUserIdAndDocumentIdAndEmbeddingJsonIsNotNull(Long userId, Long documentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DocumentChunk chunk where chunk.userId = :userId and chunk.documentId = :documentId")
    int deleteByUserIdAndDocumentId(@Param("userId") Long userId, @Param("documentId") Long documentId);
}
