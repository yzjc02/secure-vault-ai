package com.jiacheng.securevault.conversation;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class ConversationSchemaInitializer implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public ConversationSchemaInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!isPostgreSql()) {
            return;
        }
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS conversations (
                    id BIGSERIAL PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    title VARCHAR(255),
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS chat_messages (
                    id BIGSERIAL PRIMARY KEY,
                    conversation_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    role VARCHAR(32) NOT NULL,
                    content TEXT NOT NULL,
                    sources_json TEXT,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_conversations_user_id ON conversations (user_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_conversations_user_updated ON conversations (user_id, updated_at)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_chat_messages_user_id ON chat_messages (user_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_chat_messages_conversation_id ON chat_messages (conversation_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_chat_messages_conversation_created ON chat_messages (conversation_id, created_at)");
    }

    private boolean isPostgreSql() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase().contains("postgresql");
        }
    }
}
