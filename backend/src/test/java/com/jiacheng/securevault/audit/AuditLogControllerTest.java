package com.jiacheng.securevault.audit;

import com.jiacheng.securevault.audit.entity.AuditLog;
import com.jiacheng.securevault.audit.enums.AuditAction;
import com.jiacheng.securevault.audit.enums.AuditResourceType;
import com.jiacheng.securevault.audit.repository.AuditLogRepository;
import com.jiacheng.securevault.conversation.repository.ChatMessageRepository;
import com.jiacheng.securevault.conversation.repository.ConversationRepository;
import com.jiacheng.securevault.document.repository.DocumentChunkRepository;
import com.jiacheng.securevault.document.repository.DocumentRepository;
import com.jiacheng.securevault.document.service.FileStorageService;
import com.jiacheng.securevault.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
        "jwt.expiration=86400000",
        "app.file-storage.upload-dir=${java.io.tmpdir}/secure-vault-ai-module9-test-uploads",
        "app.file-storage.max-file-size=4096",
        "secure-vault.security.file-encryption.key=module9-test-key-material-32-chars-minimum",
        "app.chat.provider=deterministic",
        "app.rag.default-top-k=5",
        "app.rag.max-top-k=5"
})
@AutoConfigureMockMvc
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() throws Exception {
        auditLogRepository.deleteAll();
        chatMessageRepository.deleteAll();
        conversationRepository.deleteAll();
        documentChunkRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();
        cleanUploadRoot();
    }

    @AfterEach
    void tearDown() throws Exception {
        cleanUploadRoot();
    }

    @Test
    void shouldRequireAuthenticationForAuditLogs() throws Exception {
        mockMvc.perform(get("/api/me/audit-logs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturnOnlyCurrentUserAuditLogsAndHideSensitiveFields() throws Exception {
        String tokenA = registerAndLogin("audit_alice");
        String tokenB = registerAndLogin("audit_bob");
        Long userAId = userRepository.findByUsername("audit_alice").orElseThrow().getId();
        AuditLog sensitiveLog = new AuditLog();
        sensitiveLog.setUserId(userAId);
        sensitiveLog.setAction(AuditAction.DOCUMENT_PARSE_FAILURE);
        sensitiveLog.setResourceType(AuditResourceType.DOCUMENT);
        sensitiveLog.setResourceId(100L);
        sensitiveLog.setSuccess(false);
        sensitiveLog.setMessage("password=secret Bearer aaa.bbb.ccc filePath=C:\\uploads\\a.txt storedFilename=a.txt fullPrompt=prompt embedding=[0.1, 0.2, 0.3] encryptionKey=secret jdbc:postgresql://db /uploads/a.txt");
        auditLogRepository.save(sensitiveLog);

        MvcResult resultA = mockMvc.perform(get("/api/me/audit-logs")
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[*].action", hasItem("REGISTER_SUCCESS")))
                .andExpect(jsonPath("$.data.items[*].action", hasItem("LOGIN_SUCCESS")))
                .andExpect(jsonPath("$.data.items[*].action", hasItem("DOCUMENT_PARSE_FAILURE")))
                .andExpect(jsonPath("$.data.items[*].action", not(hasItem("RESOURCE_ACCESS_DENIED"))))
                .andReturn();
        assertNoSensitiveFields(resultA.getResponse().getContentAsString());

        mockMvc.perform(get("/api/me/audit-logs/{id}", sensitiveLog.getId())
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(get("/api/me/audit-logs/{id}", sensitiveLog.getId())
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").doesNotExist());
    }

    @Test
    void shouldRecordBusinessAuditEventsAndAccessDeniedForCurrentUser() throws Exception {
        String tokenA = registerAndLogin("chain_alice");
        String tokenB = registerAndLogin("chain_bob");
        long docId = uploadDocument(tokenA, "module9.txt",
                "Secure Vault AI module nine validates audit logs, embeddings and RAG observability.");

        mockMvc.perform(post("/api/documents/{id}/embed", docId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/chat/ask")
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "What does module nine validate?",
                                  "topK": 5,
                                  "documentId": %d
                                }
                                """.formatted(docId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/documents/{id}", docId)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(delete("/api/documents/{id}", docId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/me/audit-logs")
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].action", hasItem("DOCUMENT_UPLOAD_SUCCESS")))
                .andExpect(jsonPath("$.data.items[*].action", hasItem("DOCUMENT_PARSE_SUCCESS")))
                .andExpect(jsonPath("$.data.items[*].action", hasItem("DOCUMENT_EMBED_SUCCESS")))
                .andExpect(jsonPath("$.data.items[*].action", hasItem("RAG_ASK_SUCCESS")))
                .andExpect(jsonPath("$.data.items[*].action", hasItem("DOCUMENT_DELETE_SUCCESS")));

        mockMvc.perform(get("/api/me/audit-logs")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].action", hasItem("RESOURCE_ACCESS_DENIED")))
                .andExpect(jsonPath("$.data.items[*].resourceId", hasItem((int) docId)));
    }

    private String registerAndLogin(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s@example.com",
                                  "password": "Password123"
                                }
                                """.formatted(username, username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "Password123"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return extractString(result.getResponse().getContentAsString(), "token");
    }

    private long uploadDocument(String token, String filename, String content) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", filename, MediaType.TEXT_PLAIN_VALUE,
                                content.getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return extractLong(result.getResponse().getContentAsString(), "id");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String extractString(String json, String fieldName) {
        Matcher matcher = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Missing JSON string field: " + fieldName);
        }
        return matcher.group(1);
    }

    private long extractLong(String json, String fieldName) {
        Matcher matcher = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Missing JSON number field: " + fieldName);
        }
        return Long.parseLong(matcher.group(1));
    }

    private void assertNoSensitiveFields(String json) {
        assertThat(json)
                .doesNotContain("userId")
                .doesNotContain("password")
                .doesNotContain("token")
                .doesNotContain("Bearer")
                .doesNotContain("filePath")
                .doesNotContain("storedFilename")
                .doesNotContain("fullPrompt")
                .doesNotContain("embedding")
                .doesNotContain("encryptionKey")
                .doesNotContain("jdbc:")
                .doesNotContain("C:\\")
                .doesNotContain("/uploads/");
    }

    private void cleanUploadRoot() throws Exception {
        Path uploadRoot = fileStorageService.getUploadRoot();
        if (Files.exists(uploadRoot)) {
            try (Stream<Path> paths = Files.walk(uploadRoot)) {
                paths.sorted(Comparator.reverseOrder())
                        .filter(path -> !path.equals(uploadRoot))
                        .forEach(this::deleteQuietly);
            }
        }
        Files.createDirectories(uploadRoot);
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to clean test upload file: " + path, ex);
        }
    }
}
