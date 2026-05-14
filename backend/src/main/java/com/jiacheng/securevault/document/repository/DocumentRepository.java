package com.jiacheng.securevault.document.repository;

import com.jiacheng.securevault.document.entity.Document;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Document> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select document from Document document where document.id = :id and document.userId = :userId")
    Optional<Document> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
