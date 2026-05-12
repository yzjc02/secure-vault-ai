package com.jiacheng.securevault.document.embedding;

import com.jiacheng.securevault.document.repository.DocumentChunkRepository;
import com.jiacheng.securevault.document.repository.DocumentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
public class ChunkEmbeddingStoreConfiguration {

    @Bean
    public ChunkEmbeddingStore chunkEmbeddingStore(DataSource dataSource,
                                                   JdbcTemplate jdbcTemplate,
                                                   DocumentChunkRepository documentChunkRepository,
                                                   DocumentRepository documentRepository) throws SQLException {
        if (isPostgreSql(dataSource)) {
            return new PgvectorChunkEmbeddingStore(jdbcTemplate);
        }
        return new FallbackChunkEmbeddingStore(documentChunkRepository, documentRepository);
    }

    private boolean isPostgreSql(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase().contains("postgresql");
        }
    }
}
