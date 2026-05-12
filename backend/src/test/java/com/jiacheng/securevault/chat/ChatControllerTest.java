package com.jiacheng.securevault.chat;

import com.jiacheng.securevault.conversation.entity.ChatMessage;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
        "jwt.expiration=86400000",
        "app.file-storage.upload-dir=${java.io.tmpdir}/secure-vault-ai-module7-test-uploads",
        "app.file-storage.max-file-size=4096",
        "app.chat.provider=deterministic",
        "app.rag.default-top-k=5",
        "app.rag.max-top-k=5"
})
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    void shouldRejectUnauthenticatedAndInvalidAskRequests() throws Exception {
        mockMvc.perform(post("/api/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"hello"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        String token = registerAndLogin("alice");

        mockMvc.perform(post("/api/chat/ask")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/chat/ask")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"hello","topK":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/chat/ask")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"hello","topK":6}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldAskWithSourcesCreateAndContinueConversationWithoutLeakingSensitiveFields() throws Exception {
        String token = registerAndLogin("alice");
        long docId = uploadAndEmbed(token, "module7.txt",
                "Secure Vault AI module seven validates RAG question answering with sources and strict user isolation.");

        MvcResult firstAsk = mockMvc.perform(post("/api/chat/ask")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "What does module seven validate?",
                                  "topK": 5,
                                  "documentId": %d
                                }
                                """.formatted(docId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.conversationId", notNullValue()))
                .andExpect(jsonPath("$.data.userMessageId", notNullValue()))
                .andExpect(jsonPath("$.data.assistantMessageId", notNullValue()))
                .andExpect(jsonPath("$.data.answer").value(containsString("根据你的知识库片段")))
                .andExpect(jsonPath("$.data.sources", hasSize(1)))
                .andExpect(jsonPath("$.data.sources[0].sourceId").value("S1"))
                .andExpect(jsonPath("$.data.sources[0].documentId").value(docId))
                .andExpect(jsonPath("$.data.sources[0].contentPreview").value(containsString("module seven validates")))
                .andExpect(jsonPath("$.data.sources[0].embedding").doesNotExist())
                .andExpect(jsonPath("$.data.sources[0].filePath").doesNotExist())
                .andExpect(jsonPath("$.data.sources[0].userId").doesNotExist())
                .andExpect(jsonPath("$.data.fullPrompt").doesNotExist())
                .andReturn();

        String firstJson = firstAsk.getResponse().getContentAsString();
        assertThat(firstJson).doesNotContain("\"embedding\":");
        assertThat(firstJson).doesNotContain("\"filePath\"");
        assertThat(firstJson).doesNotContain("\"userId\"");
        assertThat(firstJson).doesNotContain("\"storedFilename\"");
        assertThat(firstJson).doesNotContain("\"fullPrompt\"");

        long conversationId = extractLong(firstJson, "conversationId");

        mockMvc.perform(post("/api/chat/ask")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "Continue this conversation",
                                  "conversationId": %d,
                                  "documentId": %d
                                }
                                """.formatted(conversationId, docId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").value(conversationId))
                .andExpect(jsonPath("$.data.sources", hasSize(1)));

        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].conversationId").value(conversationId))
                .andExpect(jsonPath("$.data[0].messageCount").value(4))
                .andExpect(jsonPath("$.data[0].userId").doesNotExist());

        MvcResult messagesResult = mockMvc.perform(get("/api/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").value(conversationId))
                .andExpect(jsonPath("$.data.messages", hasSize(4)))
                .andExpect(jsonPath("$.data.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.data.messages[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.messages[1].sources", hasSize(1)))
                .andExpect(jsonPath("$.data.messages[1].sources[0].documentId").value(docId))
                .andExpect(jsonPath("$.data.messages[1].userId").doesNotExist())
                .andReturn();

        assertThat(messagesResult.getResponse().getContentAsString()).doesNotContain("\"filePath\"");
        assertThat(messagesResult.getResponse().getContentAsString()).doesNotContain("\"embedding\":");

        ChatMessage assistantMessage = chatMessageRepository.findById(extractLong(firstJson, "assistantMessageId"))
                .orElseThrow();
        assertThat(assistantMessage.getSourcesJson()).contains("\"sourceId\":\"S1\"");
    }

    @Test
    void shouldEnforceUserIsolationForDocumentsConversationsAndSearchSources() throws Exception {
        String tokenA = registerAndLogin("alice");
        String tokenB = registerAndLogin("bob");
        long docA = uploadAndEmbed(tokenA, "alice.txt", "alice private module seven isolation content");

        MvcResult askA = mockMvc.perform(post("/api/chat/ask")
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"module seven isolation","documentId":%d}
                                """.formatted(docA)))
                .andExpect(status().isOk())
                .andReturn();
        long conversationA = extractLong(askA.getResponse().getContentAsString(), "conversationId");

        mockMvc.perform(post("/api/chat/ask")
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"module seven isolation","documentId":%d}
                                """.formatted(docA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(post("/api/chat/ask")
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"continue","conversationId":%d}
                                """.formatted(conversationA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(get("/api/conversations/{id}/messages", conversationA)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        mockMvc.perform(post("/api/chat/ask")
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"module seven isolation"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sources", hasSize(0)))
                .andExpect(jsonPath("$.data.answer").value(DeterministicChatModelClient.NO_SOURCES_ANSWER));
    }

    @Test
    void shouldReturnFallbackAndSaveMessagesWhenNoChunksExist() throws Exception {
        String token = registerAndLogin("alice");

        MvcResult result = mockMvc.perform(post("/api/chat/ask")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"What is in my empty vault?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value(DeterministicChatModelClient.NO_SOURCES_ANSWER))
                .andExpect(jsonPath("$.data.sources", hasSize(0)))
                .andReturn();

        long conversationId = extractLong(result.getResponse().getContentAsString(), "conversationId");
        mockMvc.perform(get("/api/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages", hasSize(2)))
                .andExpect(jsonPath("$.data.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.data.messages[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.messages[1].sources", hasSize(0)));
    }

    @Test
    void shouldKeepModuleSixSearchWorkingAfterModuleSevenChanges() throws Exception {
        String token = registerAndLogin("alice");
        long docId = uploadAndEmbed(token, "search.txt", "module six regression semantic search content");

        mockMvc.perform(post("/api/documents/search-chunks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"semantic search","topK":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].documentId").value(docId))
                .andExpect(jsonPath("$.data[0].content").value(containsString("semantic search")))
                .andExpect(jsonPath("$.data[0].filePath").doesNotExist())
                .andExpect(jsonPath("$.data[0].userId").doesNotExist())
                .andExpect(jsonPath("$.data[0].embedding").doesNotExist());
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
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "Password123"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();

        return extractString(result.getResponse().getContentAsString(), "token");
    }

    private long uploadAndEmbed(String token, String filename, String content) throws Exception {
        MvcResult upload = mockMvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", filename, MediaType.TEXT_PLAIN_VALUE,
                                content.getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        long docId = extractLong(upload.getResponse().getContentAsString(), "id");
        mockMvc.perform(post("/api/documents/{id}/embed", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EMBEDDED"))
                .andExpect(jsonPath("$.data.embeddedChunkCount", greaterThan(0)));
        return docId;
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
