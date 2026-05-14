package com.jiacheng.securevault.document;

import com.jiacheng.securevault.audit.entity.AuditLog;
import com.jiacheng.securevault.audit.enums.AuditAction;
import com.jiacheng.securevault.audit.repository.AuditLogRepository;
import com.jiacheng.securevault.conversation.repository.ChatMessageRepository;
import com.jiacheng.securevault.conversation.repository.ConversationRepository;
import com.jiacheng.securevault.document.entity.Document;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
        "jwt.expiration=86400000",
        "app.file-storage.upload-dir=${java.io.tmpdir}/secure-vault-ai-reindex-test-uploads",
        "app.file-storage.max-file-size=4096",
        "secure-vault.security.file-encryption.key=module11-test-key-material-32-chars-minimum",
        "app.chat.provider=deterministic",
        "app.rag.default-top-k=5",
        "app.rag.max-top-k=5"
})
@AutoConfigureMockMvc
class DocumentReindexControllerTest {

    private static final String KNOWN_PHRASE = "manual reindex, chunk rebuild, embedding regeneration, and user isolation";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

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
    void shouldReindexDocumentAndKeepRagEvidenceTrailWorking() throws Exception {
        String token = registerAndLogin("reindexalice");
        long documentId = uploadDocument(token, "module11.txt", moduleElevenText());
        embedDocument(token, documentId);

        MvcResult beforeAsk = ask(token, documentId, "What does module eleven verify?")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value(containsString("根据你的知识库片段")))
                .andExpect(jsonPath("$.data.sources", hasSize(1)))
                .andExpect(jsonPath("$.data.sources[0].documentId").value(documentId))
                .andExpect(jsonPath("$.data.sources[0].documentTitle").value("module11.txt"))
                .andExpect(jsonPath("$.data.sources[0].originalFilename").value("module11.txt"))
                .andExpect(jsonPath("$.data.sources[0].chunkIndex", notNullValue()))
                .andExpect(jsonPath("$.data.sources[0].score", notNullValue()))
                .andExpect(jsonPath("$.data.sources[0].snippet").value(containsString("module eleven")))
                .andExpect(jsonPath("$.data.sources[0].createdAt", notNullValue()))
                .andReturn();
        assertNoSensitiveFields(beforeAsk.getResponse().getContentAsString());

        long chunkCountBefore = documentChunkRepository.countByUserIdAndDocumentId(currentUserId(documentId), documentId);

        MvcResult reindex = mockMvc.perform(post("/api/documents/{documentId}/reindex", documentId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.documentId").value(documentId))
                .andExpect(jsonPath("$.data.title").value("module11.txt"))
                .andExpect(jsonPath("$.data.status").value(Document.STATUS_EMBEDDED))
                .andExpect(jsonPath("$.data.chunkCount", greaterThan(0)))
                .andExpect(jsonPath("$.data.embeddedChunkCount", greaterThan(0)))
                .andExpect(jsonPath("$.data.reindexedAt", notNullValue()))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.filePath").doesNotExist())
                .andReturn();

        String reindexJson = reindex.getResponse().getContentAsString();
        assertNoSensitiveFields(reindexJson);
        int chunkCountAfter = extractInt(reindexJson, "chunkCount");
        assertThat(chunkCountAfter).isEqualTo((int) chunkCountBefore);
        assertThat(documentChunkRepository.countByUserIdAndDocumentId(currentUserId(documentId), documentId))
                .isEqualTo(chunkCountAfter);
        assertThat(documentChunkRepository.countByUserIdAndDocumentIdAndEmbeddingJsonIsNotNull(currentUserId(documentId), documentId))
                .isEqualTo(chunkCountAfter);

        MvcResult afterAsk = ask(token, documentId, "What should evidence trail cite after reindex?")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value(containsString("根据你的知识库片段")))
                .andExpect(jsonPath("$.data.sources", hasSize(1)))
                .andExpect(jsonPath("$.data.sources[0].documentId").value(documentId))
                .andExpect(jsonPath("$.data.sources[0].documentTitle").value("module11.txt"))
                .andExpect(jsonPath("$.data.sources[0].originalFilename").value("module11.txt"))
                .andExpect(jsonPath("$.data.sources[0].chunkIndex", notNullValue()))
                .andExpect(jsonPath("$.data.sources[0].score", notNullValue()))
                .andExpect(jsonPath("$.data.sources[0].snippet").value(containsString("evidence trail")))
                .andExpect(jsonPath("$.data.sources[0].createdAt", notNullValue()))
                .andReturn();
        assertNoSensitiveFields(afterAsk.getResponse().getContentAsString());

        int chunkIndex = extractInt(afterAsk.getResponse().getContentAsString(), "chunkIndex");
        MvcResult chunkDetail = mockMvc.perform(get("/api/documents/{documentId}/chunks/{chunkIndex}", documentId, chunkIndex)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value(containsString(KNOWN_PHRASE)))
                .andExpect(jsonPath("$.data.embedding").doesNotExist())
                .andReturn();
        assertNoSensitiveFields(chunkDetail.getResponse().getContentAsString());

        assertThat(auditLogRepository.findAll())
                .anySatisfy(log -> {
                    assertThat(log.getAction()).isEqualTo(AuditAction.DOCUMENT_REINDEX);
                    assertThat(log.isSuccess()).isTrue();
                    assertThat(log.getResourceId()).isEqualTo(documentId);
                    assertThat(log.getMessage())
                            .contains("oldStatus=EMBEDDED")
                            .contains("newStatus=EMBEDDED")
                            .contains("deletedChunkCount=")
                            .contains("embeddedChunkCount=");
                });
    }

    @Test
    void shouldReturn404ForCrossUserReindexWithoutChangingOwnerDocument() throws Exception {
        String tokenA = registerAndLogin("reindexownera");
        String tokenB = registerAndLogin("reindexownerb");
        long documentId = uploadDocument(tokenA, "owner-a.txt", moduleElevenText());
        embedDocument(tokenA, documentId);
        Long ownerId = currentUserId(documentId);
        long chunkCountBefore = documentChunkRepository.countByUserIdAndDocumentId(ownerId, documentId);

        MvcResult crossUser = mockMvc.perform(post("/api/documents/{documentId}/reindex", documentId)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andReturn();
        assertNoSensitiveFields(crossUser.getResponse().getContentAsString());

        Document document = documentRepository.findById(documentId).orElseThrow();
        assertThat(document.getStatus()).isEqualTo(Document.STATUS_EMBEDDED);
        assertThat(documentChunkRepository.countByUserIdAndDocumentId(ownerId, documentId))
                .isEqualTo(chunkCountBefore);
    }

    @Test
    void shouldRequireAuthenticationForReindex() throws Exception {
        mockMvc.perform(post("/api/documents/{documentId}/reindex", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturn400WhenExtractedTextIsEmpty() throws Exception {
        String token = registerAndLogin("reindexempty");
        long documentId = createDocument(token, "empty extracted text");

        MvcResult result = mockMvc.perform(post("/api/documents/{documentId}/reindex", documentId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("文档尚未解析，无法重新索引"))
                .andReturn();
        assertNoSensitiveFields(result.getResponse().getContentAsString());

        Document document = documentRepository.findById(documentId).orElseThrow();
        assertThat(document.getStatus()).isEqualTo(Document.STATUS_CREATED);
        assertThat(documentChunkRepository.countByUserIdAndDocumentId(document.getUserId(), documentId)).isZero();
    }

    @Test
    void shouldKeepChunkCountStableAcrossRepeatedReindexCalls() throws Exception {
        String token = registerAndLogin("reindexrepeat");
        long documentId = uploadDocument(token, "repeat.txt", moduleElevenText());
        embedDocument(token, documentId);

        MvcResult first = reindex(token, documentId);
        int firstChunkCount = extractInt(first.getResponse().getContentAsString(), "chunkCount");
        MvcResult second = reindex(token, documentId);
        int secondChunkCount = extractInt(second.getResponse().getContentAsString(), "chunkCount");

        Long ownerId = currentUserId(documentId);
        assertThat(secondChunkCount).isEqualTo(firstChunkCount);
        assertThat(documentChunkRepository.countByUserIdAndDocumentId(ownerId, documentId))
                .isEqualTo(secondChunkCount);
        assertThat(documentChunkRepository.countByUserIdAndDocumentIdAndEmbeddingJsonIsNotNull(ownerId, documentId))
                .isEqualTo(secondChunkCount);
        assertNoSensitiveFields(second.getResponse().getContentAsString());

        ask(token, documentId, "What remains available after repeated reindex?")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sources", hasSize(1)))
                .andExpect(jsonPath("$.data.sources[0].documentId").value(documentId))
                .andExpect(jsonPath("$.data.sources[0].snippet").value(containsString("reindex")));
    }

    @Test
    void shouldRejectReindexWhenDocumentIsAlreadyReindexing() throws Exception {
        String token = registerAndLogin("reindexbusy");
        long documentId = uploadDocument(token, "busy.txt", moduleElevenText());
        Document document = documentRepository.findById(documentId).orElseThrow();
        document.setStatus(Document.STATUS_REINDEXING);
        documentRepository.save(document);

        mockMvc.perform(post("/api/documents/{documentId}/reindex", documentId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("文档正在重新索引，请勿重复提交"));
    }

    private MvcResultActions ask(String token, long documentId, String question) throws Exception {
        return new MvcResultActions(mockMvc.perform(post("/api/chat/ask")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "question": "%s",
                          "documentId": %d,
                          "topK": 5
                        }
                        """.formatted(question, documentId))));
    }

    private MvcResult reindex(String token, long documentId) throws Exception {
        return mockMvc.perform(post("/api/documents/{documentId}/reindex", documentId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(Document.STATUS_EMBEDDED))
                .andExpect(jsonPath("$.data.chunkCount", greaterThan(0)))
                .andExpect(jsonPath("$.data.embeddedChunkCount", greaterThan(0)))
                .andReturn();
    }

    private void embedDocument(String token, long documentId) throws Exception {
        mockMvc.perform(post("/api/documents/{documentId}/embed", documentId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(Document.STATUS_EMBEDDED));
    }

    private String registerAndLogin(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s@example.com",
                                  "password": "Password123!"
                                }
                                """.formatted(username, username)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "Password123!"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();

        return extractString(result.getResponse().getContentAsString(), "token");
    }

    private long createDocument(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/documents")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "test"
                                }
                                """.formatted(title).getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andReturn();

        return extractLong(result.getResponse().getContentAsString(), "id");
    }

    private long uploadDocument(String token, String filename, String content) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", filename, MediaType.TEXT_PLAIN_VALUE,
                                content.getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CHUNKED"))
                .andReturn();

        return extractLong(result.getResponse().getContentAsString(), "id");
    }

    private String moduleElevenText() {
        return """
                Secure Vault AI module eleven reindex test.
                This document verifies %s.
                After reindex, RAG evidence trail must still cite document id, chunk index, similarity score, and snippet.
                """.formatted(KNOWN_PHRASE);
    }

    private Long currentUserId(long documentId) {
        return documentRepository.findById(documentId).orElseThrow().getUserId();
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

    private int extractInt(String json, String fieldName) {
        Matcher matcher = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new IllegalStateException("Missing JSON number field: " + fieldName);
        }
        return Integer.parseInt(matcher.group(1));
    }

    private void assertNoSensitiveFields(String json) {
        assertThat(json)
                .doesNotContain("filePath")
                .doesNotContain("storedFilename")
                .doesNotContain("userId")
                .doesNotContain("embeddingJson")
                .doesNotContain("embedding_json")
                .doesNotContain("\"embedding\"")
                .doesNotContain("fullPrompt")
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
        } catch (Exception ignored) {
        }
    }

    private static class MvcResultActions {
        private final org.springframework.test.web.servlet.ResultActions delegate;

        private MvcResultActions(org.springframework.test.web.servlet.ResultActions delegate) {
            this.delegate = delegate;
        }

        private MvcResultActions andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
            delegate.andExpect(matcher);
            return this;
        }

        private MvcResult andReturn() {
            return delegate.andReturn();
        }
    }
}
