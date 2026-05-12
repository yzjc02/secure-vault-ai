package com.jiacheng.securevault.document;

import com.jiacheng.securevault.document.embedding.ChunkEmbeddingStore;
import com.jiacheng.securevault.document.embedding.EmbeddingClient;
import com.jiacheng.securevault.document.embedding.EmbeddingException;
import com.jiacheng.securevault.document.embedding.EmbeddingProperties;
import com.jiacheng.securevault.document.entity.Document;
import com.jiacheng.securevault.document.entity.DocumentChunk;
import com.jiacheng.securevault.document.repository.DocumentChunkRepository;
import com.jiacheng.securevault.document.repository.DocumentRepository;
import com.jiacheng.securevault.document.service.DocumentEmbeddingService;
import com.jiacheng.securevault.exception.BusinessException;
import com.jiacheng.securevault.security.AccessControlService;
import com.jiacheng.securevault.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentEmbeddingServiceTest {

    @Test
    void shouldMarkDocumentFailedWhenProviderThrowsSafeException() {
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentChunkRepository chunkRepository = mock(DocumentChunkRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        AccessControlService accessControlService = mock(AccessControlService.class);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        ChunkEmbeddingStore chunkEmbeddingStore = mock(ChunkEmbeddingStore.class);
        EmbeddingProperties properties = new EmbeddingProperties();
        DocumentEmbeddingService service = new DocumentEmbeddingService(
                documentRepository,
                chunkRepository,
                currentUserService,
                accessControlService,
                embeddingClient,
                chunkEmbeddingStore,
                properties
        );

        Document document = document(10L, 1L);
        DocumentChunk chunk = chunk(100L, 10L, 1L);
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(accessControlService.requireOwnedDocument(10L, 1L)).thenReturn(document);
        when(chunkRepository.findAllByUserIdAndDocumentIdOrderByChunkIndexAsc(1L, 10L)).thenReturn(List.of(chunk));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(embeddingClient.embed("chunk content")).thenThrow(new EmbeddingException("Embedding model request failed"));

        assertThatThrownBy(() -> service.embedDocument(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Embedding model request failed");

        assertThat(document.getStatus()).isEqualTo(Document.STATUS_FAILED);
        assertThat(document.getErrorMessage()).isEqualTo("Embedding model request failed");
    }

    private Document document(Long id, Long userId) {
        Document document = new Document();
        document.setId(id);
        document.setUserId(userId);
        document.setTitle("doc");
        document.setStatus(Document.STATUS_CHUNKED);
        document.setChunkCount(1);
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
