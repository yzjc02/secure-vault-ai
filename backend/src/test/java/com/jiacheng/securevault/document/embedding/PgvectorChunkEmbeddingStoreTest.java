package com.jiacheng.securevault.document.embedding;

import com.jiacheng.securevault.document.dto.SimilarChunkResponse;
import com.jiacheng.securevault.document.entity.DocumentChunk;
import com.jiacheng.securevault.document.repository.DocumentChunkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PgvectorChunkEmbeddingStoreTest {

    @Test
    void shouldUseRepositoryChunkContentWhenNativePgvectorRowReturnsLobId() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);

        DocumentChunk chunk = new DocumentChunk();
        chunk.setContent("Readable pgvector source preview text from the stored document chunk.");
        when(documentChunkRepository.findByIdAndUserId(42L, 7L)).thenReturn(Optional.of(chunk));

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<SimilarChunkResponse> mapper = invocation.getArgument(1, RowMapper.class);
                    ResultSet resultSet = mock(ResultSet.class);
                    when(resultSet.getLong("chunk_id")).thenReturn(42L);
                    when(resultSet.getLong("document_id")).thenReturn(11L);
                    when(resultSet.getString("document_title")).thenReturn("Readable Sources");
                    when(resultSet.getString("original_filename")).thenReturn("sources.txt");
                    when(resultSet.getInt("chunk_index")).thenReturn(4);
                    when(resultSet.getDouble("score")).thenReturn(0.066d);
                    when(resultSet.getString("content")).thenReturn("16973");
                    when(resultSet.getTimestamp("embedded_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
                    return List.of(mapper.mapRow(resultSet, 0));
                });

        PgvectorChunkEmbeddingStore store = new PgvectorChunkEmbeddingStore(jdbcTemplate, documentChunkRepository);

        List<SimilarChunkResponse> responses = store.search(7L, "[0.1,0.2]", List.of(0.1d, 0.2d), null, 5);

        assertThat(responses).hasSize(1);
        SimilarChunkResponse response = responses.get(0);
        assertThat(response.getDocumentId()).isEqualTo(11L);
        assertThat(response.getChunkIndex()).isEqualTo(4);
        assertThat(response.getScore()).isEqualTo(0.066d);
        assertThat(response.content()).contains("Readable pgvector source preview text");
        assertThat(response.getContentPreview())
                .contains("Readable pgvector source preview text")
                .doesNotContain("16973");
    }
}
