package com.jiacheng.securevault.document;

import com.jiacheng.securevault.document.repository.DocumentRepository;
import com.jiacheng.securevault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
        "jwt.expiration=86400000"
})
@AutoConfigureMockMvc
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldReturn401WhenListDocumentsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString("charset=UTF-8")))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldCreateDocumentForLoginUser() throws Exception {
        String token = registerAndLogin("alice");

        mockMvc.perform(post("/api/documents")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  Spring Security 学习笔记  ",
                                  "description": "记录模块一 JWT 鉴权流程"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("Spring Security 学习笔记"))
                .andExpect(jsonPath("$.data.description").value("记录模块一 JWT 鉴权流程"))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.userId").doesNotExist());
    }

    @Test
    void createDocument_withChineseTitleAndDescription_shouldReturnOriginalChineseText() throws Exception {
        String token = registerAndLogin("alice");
        String content = """
                {
                  "title": "Spring Security 学习笔记",
                  "description": "记录模块一 JWT 鉴权流程"
                }
                """;

        mockMvc.perform(post("/api/documents")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content(content.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("charset=UTF-8")))
                .andExpect(jsonPath("$.data.title").value("Spring Security 学习笔记"))
                .andExpect(jsonPath("$.data.description").value("记录模块一 JWT 鉴权流程"));
    }

    @Test
    void listDocuments_withChineseContent_shouldReturnOriginalChineseText() throws Exception {
        String token = registerAndLogin("alice");
        createDocument(token, "Spring Security 学习笔记", "记录模块一 JWT 鉴权流程");

        mockMvc.perform(get("/api/documents")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("charset=UTF-8")))
                .andExpect(jsonPath("$.data[0].title").value("Spring Security 学习笔记"))
                .andExpect(jsonPath("$.data[0].description").value("记录模块一 JWT 鉴权流程"));
    }

    @Test
    void shouldReturn400WhenTitleIsBlank() throws Exception {
        String token = registerAndLogin("alice");

        mockMvc.perform(post("/api/documents")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "   ",
                                  "description": "empty title"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("charset=UTF-8")))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("标题不能为空"))
                .andExpect(jsonPath("$.message").value(not(containsString("å"))))
                .andExpect(jsonPath("$.message").value(not(containsString("è"))))
                .andExpect(jsonPath("$.message").value(not(containsString("ç"))));
    }

    @Test
    void shouldReturnUtf8ContentTypeWhenRequestBodyMalformed() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("charset=UTF-8")))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求体格式错误"));
    }

    @Test
    void shouldOnlyListCurrentUserDocuments() throws Exception {
        String tokenA = registerAndLogin("alice");
        String tokenB = registerAndLogin("bob");
        createDocument(tokenA, "Alice Document");
        createDocument(tokenB, "Bob Document");

        mockMvc.perform(get("/api/documents")
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Alice Document"));
    }

    @Test
    void shouldReturn404WhenOtherUserGetsDocument() throws Exception {
        String tokenA = registerAndLogin("alice");
        String tokenB = registerAndLogin("bob");
        long docA = createDocument(tokenA, "Alice Document");

        mockMvc.perform(get("/api/documents/{id}", docA)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("文档不存在"));
    }

    @Test
    void shouldReturn404WhenOtherUserUpdatesDocument() throws Exception {
        String tokenA = registerAndLogin("alice");
        String tokenB = registerAndLogin("bob");
        long docA = createDocument(tokenA, "Alice Document");

        mockMvc.perform(put("/api/documents/{id}", docA)
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Bob Update",
                                  "description": "should not work"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("文档不存在"));
    }

    @Test
    void shouldReturn404WhenOtherUserDeletesDocument() throws Exception {
        String tokenA = registerAndLogin("alice");
        String tokenB = registerAndLogin("bob");
        long docA = createDocument(tokenA, "Alice Document");

        mockMvc.perform(delete("/api/documents/{id}", docA)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("文档不存在"));
    }

    @Test
    void shouldDeleteOwnDocument() throws Exception {
        String token = registerAndLogin("alice");
        long docId = createDocument(token, "Alice Document");

        mockMvc.perform(delete("/api/documents/{id}", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", nullValue()));

        mockMvc.perform(get("/api/documents/{id}", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
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

    private long createDocument(String token, String title) throws Exception {
        return createDocument(token, title, "test document");
    }

    private long createDocument(String token, String title, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/documents")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "%s"
                                }
                                """.formatted(title, description).getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
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
}
