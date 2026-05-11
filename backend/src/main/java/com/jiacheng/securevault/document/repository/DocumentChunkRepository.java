package com.jiacheng.securevault.document.repository;

import com.jiacheng.securevault.document.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findAllByUserIdAndDocumentIdOrderByChunkIndexAsc(Long userId, Long documentId);

    long countByUserIdAndDocumentId(Long userId, Long documentId);

    void deleteByUserIdAndDocumentId(Long userId, Long documentId);
}
