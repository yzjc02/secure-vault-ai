package com.jiacheng.securevault.audit;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class AuditLogSchemaInitializer implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public AuditLogSchemaInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!isPostgreSql()) {
            return;
        }
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs (user_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_user_created_at ON audit_logs (user_id, created_at DESC)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs (action)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_resource ON audit_logs (resource_type, resource_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_success ON audit_logs (success)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at DESC)");
    }

    private boolean isPostgreSql() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase().contains("postgresql");
        }
    }
}
