package com.jiacheng.securevault.document.embedding;

import com.jiacheng.securevault.document.dto.SimilarChunkResponse;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PgvectorChunkEmbeddingStore implements ChunkEmbeddingStore {

    private final JdbcTemplate jdbcTemplate;

    public PgvectorChunkEmbeddingStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveEmbedding(Long userId, Long chunkId, List<Double> embedding, String model, int dimension) {
        String vector = EmbeddingVectorUtils.toPgvectorString(embedding, dimension);
        String embeddingJson = EmbeddingVectorUtils.toJson(embedding, dimension);
        int updated = jdbcTemplate.update("""
                UPDATE document_chunks
                SET embedding = ?::vector,
                    embedding_json = ?,
                    embedding_model = ?,
                    embedding_dimension = ?,
                    embedded_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """, vector, embeddingJson, model, dimension, chunkId, userId);
        if (updated == 0) {
            throw new EmbeddingException("Embedding chunk update failed");
        }
    }

    @Override
    public int countEmbeddedChunks(Long userId, Long documentId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM document_chunks
                WHERE user_id = ? AND document_id = ? AND embedding IS NOT NULL
                """, Integer.class, userId, documentId);
        return count == null ? 0 : count;
    }

    @Override
    public List<SimilarChunkResponse> search(Long userId,
                                             String queryVectorString,
                                             List<Double> queryEmbedding,
                                             Long documentId,
                                             int topK) {
        List<Object> args = new ArrayList<>();
        args.add(queryVectorString);
        args.add(userId);
        StringBuilder sql = new StringBuilder("""
                SELECT c.id AS chunk_id,
                       c.document_id,
                       d.title AS document_title,
                       d.original_filename,
                       c.chunk_index,
                       GREATEST(0, LEAST(1, 1 - (c.embedding <=> ?::vector))) AS score,
                       c.content,
                       c.embedding_model,
                       c.embedded_at
                FROM document_chunks c
                JOIN documents d ON d.id = c.document_id AND d.user_id = c.user_id
                WHERE c.user_id = ? AND c.embedding IS NOT NULL
                """);
        if (documentId != null) {
            sql.append(" AND c.document_id = ?");
            args.add(documentId);
        }
        sql.append(" ORDER BY c.embedding <=> ?::vector LIMIT ?");
        args.add(queryVectorString);
        args.add(topK);
        return jdbcTemplate.query(sql.toString(), this::mapRow, args.toArray());
    }

    private SimilarChunkResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SimilarChunkResponse(
                rs.getLong("chunk_id"),
                rs.getLong("document_id"),
                rs.getString("document_title"),
                rs.getString("original_filename"),
                rs.getInt("chunk_index"),
                rs.getDouble("score"),
                rs.getString("content"),
                rs.getString("embedding_model"),
                toLocalDateTime(rs.getTimestamp("embedded_at"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
