package com.jiacheng.securevault.document;

import com.jiacheng.securevault.audit.enums.AuditAction;
import com.jiacheng.securevault.audit.enums.AuditResourceType;
import com.jiacheng.securevault.audit.service.AuditLogService;
import com.jiacheng.securevault.document.embedding.ChunkEmbeddingStore;
import com.jiacheng.securevault.document.embedding.EmbeddingClient;
import com.jiacheng.securevault.document.embedding.EmbeddingException;
import com.jiacheng.securevault.document.entity.Document;
import com.jiacheng.securevault.document.entity.DocumentChunk;
import com.jiacheng.securevault.document.repository.DocumentChunkRepository;
import com.jiacheng.securevault.document.repository.DocumentRepository;
import com.jiacheng.securevault.document.service.DocumentReindexService;
import com.jiacheng.securevault.document.service.TextChunkingService;
import com.jiacheng.securevault.exception.BusinessException;
import com.jiacheng.securevault.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentReindexServiceTest {

    @Test
    void shouldMarkDocumentReindexFailedAndAuditWhenEmbeddingFails() {
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentChunkRepository chunkRepository = mock(DocumentChunkRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        TextChunkingService textChunkingService = mock(TextChunkingService.class);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        ChunkEmbeddingStore chunkEmbeddingStore = mock(ChunkEmbeddingStore.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        PlatformTransactionManager transactionManager = transactionManager();

        DocumentReindexService service = new DocumentReindexService(
                documentRepository,
                chunkRepository,
                currentUserService,
                textChunkingService,
                embeddingClient,
                chunkEmbeddingStore,
                auditLogService,
                transactionManager
        );

        Document document = document(10L, 1L);
        TextChunkingService.ChunkCandidate candidate = new TextChunkingService.ChunkCandidate(
                0, "chunk content", 13, 2, "hash", 0, 13);
        DocumentChunk chunk = chunk(100L, 10L, 1L);

        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(documentRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chunkRepository.deleteByUserIdAndDocumentIdDirectly(1L, 10L)).thenReturn(1);
        when(textChunkingService.split("existing extracted text")).thenReturn(List.of(candidate));
        when(chunkRepository.saveAll(any())).thenReturn(List.of(chunk));
        when(embeddingClient.embed("chunk content")).thenThrow(new EmbeddingException("provider path C:\\secret should not leak"));

        assertThatThrownBy(() -> service.reindexDocument(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文档重新索引失败")
                .extracting("code")
                .isEqualTo(500);

        assertThat(document.getStatus()).isEqualTo(Document.STATUS_REINDEX_FAILED);
        assertThat(document.getErrorMessage()).isEqualTo("文档重新索引失败");
        verify(chunkRepository).deleteByUserIdAndDocumentIdDirectly(1L, 10L);
        verify(auditLogService).recordForUser(eq(1L),
                eq(AuditAction.DOCUMENT_REINDEX),
                eq(AuditResourceType.DOCUMENT),
                eq(10L),
                eq(false),
                contains("newStatus=REINDEX_FAILED"));
    }

    @Test
    void shouldRecordSafeSuccessAuditWithBulkDeletedChunkCount() {
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentChunkRepository chunkRepository = mock(DocumentChunkRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        TextChunkingService textChunkingService = mock(TextChunkingService.class);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        ChunkEmbeddingStore chunkEmbeddingStore = mock(ChunkEmbeddingStore.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        PlatformTransactionManager transactionManager = transactionManager();

        DocumentReindexService service = new DocumentReindexService(
                documentRepository,
                chunkRepository,
                currentUserService,
                textChunkingService,
                embeddingClient,
                chunkEmbeddingStore,
                auditLogService,
                transactionManager
        );

        Document document = document(10L, 1L);
        TextChunkingService.ChunkCandidate candidate = new TextChunkingService.ChunkCandidate(
                0, "chunk content", 13, 2, "hash", 0, 13);
        DocumentChunk chunk = chunk(100L, 10L, 1L);

        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(documentRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chunkRepository.deleteByUserIdAndDocumentIdDirectly(1L, 10L)).thenReturn(3);
        when(textChunkingService.split("existing extracted text")).thenReturn(List.of(candidate));
        when(chunkRepository.saveAll(any())).thenReturn(List.of(chunk));
        when(embeddingClient.embed("chunk content")).thenReturn(List.of(0.1d, 0.2d));
        when(embeddingClient.dimension()).thenReturn(2);
        when(embeddingClient.model()).thenReturn("deterministic-test");
        when(chunkEmbeddingStore.countEmbeddedChunks(1L, 10L)).thenReturn(1);

        service.reindexDocument(10L);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).recordForUser(eq(1L),
                eq(AuditAction.DOCUMENT_REINDEX),
                eq(AuditResourceType.DOCUMENT),
                eq(10L),
                eq(true),
                messageCaptor.capture());
        assertThat(messageCaptor.getValue())
                .contains("oldStatus=EMBEDDED")
                .contains("newStatus=EMBEDDED")
                .contains("deletedChunkCount=3")
                .contains("newChunkCount=1")
                .contains("embeddedChunkCount=1")
                .doesNotContain("chunk content")
                .doesNotContain("embedding");
    }

    private PlatformTransactionManager transactionManager() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return transactionManager;
    }

    private Document document(Long id, Long userId) {
        Document document = new Document();
        document.setId(id);
        document.setUserId(userId);
        document.setTitle("doc");
        document.setStatus(Document.STATUS_EMBEDDED);
        document.setExtractedText("existing extracted text");
        document.setChunkCount(1);
        document.setEmbeddedChunkCount(1);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        return document;
    }

    private DocumentChunk chunk(Long id, Long documentId, Long userId) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(id);
        chunk.setDocumentId(documentId);
        chunk.setUserId(userId);
        chunk.setChunkIndex(0);
        chunk.setContent("chunk content");
        chunk.setContentLength(13);
        chunk.setTokenCount(2);
        chunk.setContentHash("hash");
        chunk.setStartOffset(0);
        chunk.setEndOffset(13);
        return chunk;
    }
}
