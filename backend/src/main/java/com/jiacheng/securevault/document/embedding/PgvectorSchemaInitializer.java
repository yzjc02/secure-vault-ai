package com.jiacheng.securevault.document.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class PgvectorSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PgvectorSchemaInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingProperties properties;

    public PgvectorSchemaInitializer(DataSource dataSource,
                                     JdbcTemplate jdbcTemplate,
                                     EmbeddingProperties properties) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!isPostgreSql()) {
            return;
        }
        int dimension = properties.getDimension();
        if (dimension <= 0 || dimension > 8192) {
            throw new EmbeddingException("Embedding dimension is invalid");
        }
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute("ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS embedding vector(" + dimension + ")");
        jdbcTemplate.execute("ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS embedding_json TEXT");
        jdbcTemplate.execute("ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(128)");
        jdbcTemplate.execute("ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS embedding_dimension INTEGER");
        jdbcTemplate.execute("ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMP");
        jdbcTemplate.execute("ALTER TABLE documents ADD COLUMN IF NOT EXISTS embedded_chunk_count INTEGER");
        jdbcTemplate.execute("ALTER TABLE documents ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMP");
        jdbcTemplate.execute("ALTER TABLE documents ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(128)");
        jdbcTemplate.execute("ALTER TABLE documents ADD COLUMN IF NOT EXISTS embedding_dimension INTEGER");
        createIndexSafely();
        log.info("pgvector schema initialized");
    }

    private boolean isPostgreSql() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase().contains("postgresql");
        }
    }

    private void createIndexSafely() {
        try {
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding
                    ON document_chunks
                    USING ivfflat (embedding vector_cosine_ops)
                    WITH (lists = 100)
                    WHERE embedding IS NOT NULL
                    """);
        } catch (RuntimeException ex) {
            log.warn("pgvector ivfflat index skipped");
        }
    }
}
