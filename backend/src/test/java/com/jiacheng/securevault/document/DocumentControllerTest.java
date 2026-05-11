package com.jiacheng.securevault.document;

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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
        "jwt.expiration=86400000",
        "app.file-storage.upload-dir=${java.io.tmpdir}/secure-vault-ai-test-uploads",
        "app.file-storage.max-file-size=4096"
})
@AutoConfigureMockMvc
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content("""
                                {
                                  "title": "  Spring Security 学习笔记  ",
                                  "description": "记录模块一 JWT 鉴权流程"
                                }
                                """.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("Spring Security 学习笔记"))
                .andExpect(jsonPath("$.data.description").value("记录模块一 JWT 鉴权流程"))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.filePath").doesNotExist());
    }

    @Test
    void createDocument_withChineseTitleAndDescription_shouldReturnOriginalChineseText() throws Exception {
        String token = registerAndLogin("alice");

        mockMvc.perform(post("/api/documents")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content("""
                                {
                                  "title": "Spring Security 学习笔记",
                                  "description": "记录模块一 JWT 鉴权流程"
                                }
                                """.getBytes(StandardCharsets.UTF_8)))
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
                .andExpect(jsonPath("$.message").value(not(containsString("氓"))))
                .andExpect(jsonPath("$.message").value(not(containsString("猫"))))
                .andExpect(jsonPath("$.message").value(not(containsString("莽"))));
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

    @Test
    void shouldUploadTxtFileForLoginUser() throws Exception {
        String token = registerAndLogin("alice");
        MockMultipartFile file = textFile("notes.txt", "hello");

        MvcResult result = mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("notes.txt"))
                .andExpect(jsonPath("$.data.description", nullValue()))
                .andExpect(jsonPath("$.data.status").value("CHUNKED"))
                .andExpect(jsonPath("$.data.originalFilename").value("notes.txt"))
                .andExpect(jsonPath("$.data.storedFilename").value(containsString(".txt")))
                .andExpect(jsonPath("$.data.fileType").value("txt"))
                .andExpect(jsonPath("$.data.fileSize").value(5))
                .andExpect(jsonPath("$.data.contentType").value(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(jsonPath("$.data.textLength").value(5))
                .andExpect(jsonPath("$.data.parsedAt", notNullValue()))
                .andExpect(jsonPath("$.data.chunkCount", greaterThan(0)))
                .andExpect(jsonPath("$.data.chunkedAt", notNullValue()))
                .andExpect(jsonPath("$.data.errorMessage", nullValue()))
                .andExpect(jsonPath("$.data.filePath").doesNotExist())
                .andReturn();

        String storedFilename = extractString(result.getResponse().getContentAsString(), "storedFilename");
        long docId = extractLong(result.getResponse().getContentAsString(), "id");
        assertThat(Files.exists(fileStorageService.getUploadRoot().resolve(storedFilename))).isTrue();
        Document chunkedDocument = documentRepository.findById(docId).orElseThrow();
        assertThat(documentChunkRepository.countByUserIdAndDocumentId(chunkedDocument.getUserId(), docId))
                .isEqualTo(chunkedDocument.getChunkCount().longValue());

        mockMvc.perform(get("/api/documents/{id}/text", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CHUNKED"))
                .andExpect(jsonPath("$.data.textLength").value(5))
                .andExpect(jsonPath("$.data.extractedText").value("hello"))
                .andExpect(jsonPath("$.data.filePath").doesNotExist());

        mockMvc.perform(get("/api/documents/{id}/chunks", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].documentId").value(docId))
                .andExpect(jsonPath("$.data[0].chunkIndex").value(0))
                .andExpect(jsonPath("$.data[0].content").value("hello"))
                .andExpect(jsonPath("$.data[0].contentLength").value(5))
                .andExpect(jsonPath("$.data[0].tokenCount", greaterThan(0)))
                .andExpect(jsonPath("$.data[0].contentHash", notNullValue()))
                .andExpect(jsonPath("$.data[0].startOffset").value(0))
                .andExpect(jsonPath("$.data[0].endOffset").value(5))
                .andExpect(jsonPath("$.data[0].createdAt", notNullValue()))
                .andExpect(jsonPath("$.data[0].userId").doesNotExist())
                .andExpect(jsonPath("$.data[0].filePath").doesNotExist())
                .andExpect(jsonPath("$.data[0].extractedText").doesNotExist());
    }

    @Test
    void shouldAutoParseMarkdownFileForLoginUser() throws Exception {
        String token = registerAndLogin("alice");

        MvcResult result = mockMvc.perform(multipart("/api/documents/upload")
                        .file(markdownFile("notes.md", "# T\nbody"))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CHUNKED"))
                .andExpect(jsonPath("$.data.fileType").value("md"))
                .andExpect(jsonPath("$.data.textLength", greaterThan(0)))
                .andExpect(jsonPath("$.data.chunkCount", greaterThan(0)))
                .andExpect(jsonPath("$.data.chunkedAt", notNullValue()))
                .andReturn();

        long docId = extractLong(result.getResponse().getContentAsString(), "id");
        mockMvc.perform(get("/api/documents/{id}/text", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.extractedText").value(containsString("body")));
    }

    @Test
    void shouldReturnNormalChineseChunkContentForModuleFiveSmokeText() throws Exception {
        String token = registerAndLogin("alice");
        long docId = uploadDocument(token, "module-five-smoke.txt", moduleFiveSmokeText());

        mockMvc.perform(get("/api/documents/{id}/chunks", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].content").value(containsString("chunks 接口可以返回内容。")))
                .andExpect(jsonPath("$.data[0].content").value(containsString("text 接口仍然可以返回完整 extractedText。")))
                .andExpect(jsonPath("$.data[0].content").value(containsString("列表和详情接口不能暴露 extractedText 和 filePath。")))
                .andExpect(jsonPath("$.data[0].content").value(containsString("用户隔离必须生效。")))
                .andExpect(jsonPath("$.data[0].content").value(not(containsString("接接口口"))))
                .andExpect(jsonPath("$.data[0].content").value(not(containsString("可可以以"))))
                .andExpect(jsonPath("$.data[0].content").value(not(containsString("返返回回"))))
                .andExpect(jsonPath("$.data[0].content").value(not(containsString("内内容容"))))
                .andExpect(jsonPath("$.data[0].content").value(not(containsString("用用户户"))));
    }

    @Test
    void moduleFiveSmokeTextShouldStillHideFullTextFromListAndDetailButExposeTextEndpoint() throws Exception {
        String token = registerAndLogin("alice");
        long docId = uploadDocument(token, "module-five-smoke.txt", moduleFiveSmokeText());

        MvcResult listResult = mockMvc.perform(get("/api/documents")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].extractedText").doesNotExist())
                .andExpect(jsonPath("$.data[0].filePath").doesNotExist())
                .andReturn();

        mockMvc.perform(get("/api/documents/{id}", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.extractedText").doesNotExist())
                .andExpect(jsonPath("$.data.filePath").doesNotExist());

        mockMvc.perform(get("/api/documents/{id}/text", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.extractedText").value(containsString("chunks 接口可以返回内容。")));

        assertThat(listResult.getResponse().getContentAsString()).doesNotContain("chunks 接口可以返回内容。");
    }

    @Test
    void listDocumentsShouldNotReturnExtractedTextFullContent() throws Exception {
        String token = registerAndLogin("alice");
        uploadDocument(token, "secret.txt", "secretContent123");

        MvcResult result = mockMvc.perform(get("/api/documents")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("CHUNKED"))
                .andExpect(jsonPath("$.data[0].textLength").value(16))
                .andExpect(jsonPath("$.data[0].parsedAt", notNullValue()))
                .andExpect(jsonPath("$.data[0].chunkCount", greaterThan(0)))
                .andExpect(jsonPath("$.data[0].chunkedAt", notNullValue()))
                .andExpect(jsonPath("$.data[0].extractedText").doesNotExist())
                .andExpect(jsonPath("$.data[0].filePath").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("secretContent123");
    }

    @Test
    void getDocumentDetailShouldReturnPreviewButNotFullExtractedText() throws Exception {
        String token = registerAndLogin("alice");
        long docId = uploadDocument(token, "secret.txt", "secretContent123");

        mockMvc.perform(get("/api/documents/{id}", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CHUNKED"))
                .andExpect(jsonPath("$.data.textLength").value(16))
                .andExpect(jsonPath("$.data.chunkCount", greaterThan(0)))
                .andExpect(jsonPath("$.data.chunkedAt", notNullValue()))
                .andExpect(jsonPath("$.data.extractedTextPreview").value("secretContent123"))
                .andExpect(jsonPath("$.data.extractedText").doesNotExist())
                .andExpect(jsonPath("$.data.filePath").doesNotExist());
    }

    @Test
    void shouldManuallyReparseOwnUploadedDocument() throws Exception {
        String token = registerAndLogin("alice");
        long docId = uploadDocument(token, "notes.txt", "hello");

        mockMvc.perform(post("/api/documents/{id}/parse", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CHUNKED"))
                .andExpect(jsonPath("$.data.textLength").value(5))
                .andExpect(jsonPath("$.data.chunkCount", greaterThan(0)))
                .andExpect(jsonPath("$.data.chunkedAt", notNullValue()))
                .andExpect(jsonPath("$.data.errorMessage", nullValue()))
                .andExpect(jsonPath("$.data.filePath").doesNotExist());

        Document reparsedDocument = documentRepository.findById(docId).orElseThrow();
        assertThat(documentChunkRepository.countByUserIdAndDocumentId(reparsedDocument.getUserId(), docId))
                .isEqualTo(reparsedDocument.getChunkCount().longValue());
    }

    @Test
    void shouldManuallyRechunkOwnUploadedDocumentWithoutDuplicateChunks() throws Exception {
        String token = registerAndLogin("alice");
        long docId = uploadDocument(token, "notes.txt", "hello");
        Document document = documentRepository.findById(docId).orElseThrow();
        long initialCount = documentChunkRepository.countByUserIdAndDocumentId(document.getUserId(), docId);

        mockMvc.perform(post("/api/documents/{id}/chunk", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CHUNKED"))
                .andExpect(jsonPath("$.data.chunkCount").value((int) initialCount))
                .andExpect(jsonPath("$.data.chunkedAt", notNullValue()))
                .andExpect(jsonPath("$.data.filePath").doesNotExist())
                .andExpect(jsonPath("$.data.extractedText").doesNotExist());

        mockMvc.perform(post("/api/documents/{id}/chunk", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chunkCount").value((int) initialCount));

        Document rechunkedDocument = documentRepository.findById(docId).orElseThrow();
        assertThat(documentChunkRepository.countByUserIdAndDocumentId(rechunkedDocument.getUserId(), docId))
                .isEqualTo(initialCount);
    }

    @Test
    void shouldReturn400WhenChunkingCreatedDocumentWithoutExtractedText() throws Exception {
        String token = registerAndLogin("alice");
        long docId = createDocument(token, "Plain Record");

        mockMvc.perform(post("/api/documents/{id}/chunk", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldReturn404WhenChunkingMissingDocument() throws Exception {
        String token = registerAndLogin("alice");

        mockMvc.perform(post("/api/documents/{id}/chunk", 99999L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldUseCustomTitleWhenUploadTitleProvided() throws Exception {
        String token = registerAndLogin("alice");

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(textFile("notes.txt", "hello"))
                        .param("title", "我的测试文档")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("我的测试文档"))
                .andExpect(jsonPath("$.data.originalFilename").value("notes.txt"));
    }

    @Test
    void shouldReturn401WhenUploadWithoutToken() throws Exception {
        mockMvc.perform(multipart("/api/documents/upload")
                        .file(textFile("notes.txt", "hello")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturn404WhenOtherUserGetsUploadedDocument() throws Exception {
        String tokenA = registerAndLogin("alice");
        String tokenB = registerAndLogin("bob");
        long docA = uploadDocument(tokenA, "notes.txt", "hello");

        mockMvc.perform(get("/api/documents/{id}", docA)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("文档不存在"));

        mockMvc.perform(get("/api/documents/{id}/text", docA)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("文档不存在"));

        mockMvc.perform(post("/api/documents/{id}/parse", docA)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("文档不存在"));
    }

    @Test
    void shouldReturn404WhenOtherUserAccessesChunkEndpoints() throws Exception {
        String tokenA = registerAndLogin("alice2");
        String tokenB = registerAndLogin("bob2");
        long docA = uploadDocument(tokenA, "notes.txt", "hello");

        mockMvc.perform(get("/api/documents/{id}/chunks", docA)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(post("/api/documents/{id}/chunk", docA)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldReturn401WhenParseOrReadTextWithoutToken() throws Exception {
        mockMvc.perform(post("/api/documents/{id}/parse", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/documents/{id}/text", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/documents/{id}/chunk", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/documents/{id}/chunks", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturn400WhenParsingCreatedDocumentWithoutFile() throws Exception {
        String token = registerAndLogin("alice");
        long docId = createDocument(token, "Plain Record");

        mockMvc.perform(post("/api/documents/{id}/parse", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("该文档没有可解析的上传文件"));
    }

    @Test
    void shouldMarkFailedWhenLocalUploadedFileIsMissing() throws Exception {
        String token = registerAndLogin("alice");
        long docId = uploadDocument(token, "notes.txt", "hello");
        Document document = documentRepository.findById(docId).orElseThrow();
        Files.deleteIfExists(fileStorageService.getUploadRoot().resolve(document.getStoredFilename()));

        mockMvc.perform(post("/api/documents/{id}/parse", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.errorMessage").value("本地文件不存在"))
                .andExpect(jsonPath("$.data.textLength").value(0))
                .andExpect(jsonPath("$.data.parsedAt", notNullValue()))
                .andExpect(jsonPath("$.data.filePath").doesNotExist());

        Document failedDocument = documentRepository.findById(docId).orElseThrow();
        assertThat(failedDocument.getStatus()).isEqualTo("FAILED");
        assertThat(failedDocument.getErrorMessage()).isEqualTo("本地文件不存在");
    }

    @Test
    void shouldClearChunksWhenReparseFails() throws Exception {
        String token = registerAndLogin("alice3");
        long docId = uploadDocument(token, "notes.txt", "hello");
        Document document = documentRepository.findById(docId).orElseThrow();
        assertThat(documentChunkRepository.countByUserIdAndDocumentId(document.getUserId(), docId)).isGreaterThan(0);
        Files.deleteIfExists(fileStorageService.getUploadRoot().resolve(document.getStoredFilename()));

        mockMvc.perform(post("/api/documents/{id}/parse", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.chunkCount").value(0))
                .andExpect(jsonPath("$.data.chunkedAt", nullValue()));

        Document failedDocument = documentRepository.findById(docId).orElseThrow();
        assertThat(documentChunkRepository.countByUserIdAndDocumentId(failedDocument.getUserId(), docId)).isZero();
    }

    @Test
    void shouldReturn404AndKeepFileWhenOtherUserDeletesUploadedDocument() throws Exception {
        String tokenA = registerAndLogin("alice");
        String tokenB = registerAndLogin("bob");
        long docA = uploadDocument(tokenA, "notes.txt", "hello");
        String storedFilename = documentRepository.findById(docA).orElseThrow().getStoredFilename();

        mockMvc.perform(delete("/api/documents/{id}", docA)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        assertThat(Files.exists(fileStorageService.getUploadRoot().resolve(storedFilename))).isTrue();
    }

    @Test
    void shouldDeleteLocalFileWhenOwnerDeletesUploadedDocument() throws Exception {
        String token = registerAndLogin("alice");
        long docId = uploadDocument(token, "notes.txt", "hello");
        Document document = documentRepository.findById(docId).orElseThrow();
        String storedFilename = document.getStoredFilename();
        Path storedFile = fileStorageService.getUploadRoot().resolve(storedFilename);
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(documentChunkRepository.countByUserIdAndDocumentId(document.getUserId(), docId)).isGreaterThan(0);

        mockMvc.perform(delete("/api/documents/{id}", docId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        assertThat(Files.exists(storedFile)).isFalse();
        assertThat(documentChunkRepository.countByUserIdAndDocumentId(document.getUserId(), docId)).isZero();
    }

    @Test
    void shouldReturn400WhenUploadEmptyFile() throws Exception {
        String token = registerAndLogin("alice");

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(textFile("empty.txt", ""))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("上传文件不能为空"));
    }

    @Test
    void shouldReturn400WhenUploadUnsupportedExtension() throws Exception {
        String token = registerAndLogin("alice");

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "evil.exe",
                                "application/octet-stream", "bad".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不支持的文件类型，仅支持 pdf、docx、txt、md、markdown"));
    }

    @Test
    void shouldReturn400WhenUploadFileWithoutExtension() throws Exception {
        String token = registerAndLogin("alice");

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "README",
                                MediaType.TEXT_PLAIN_VALUE, "hello".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不支持的文件类型，仅支持 pdf、docx、txt、md、markdown"));
    }

    @Test
    void shouldReturn400WhenUploadFileExceedsMaxSize() throws Exception {
        String token = registerAndLogin("alice");

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(textFile("large.txt", "x".repeat(4097)))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("文件大小不能超过 4096B"));
    }

    @Test
    void shouldNotEscapeUploadDirectoryWhenOriginalFilenameContainsPathTraversal() throws Exception {
        String token = registerAndLogin("alice");
        MvcResult result = mockMvc.perform(multipart("/api/documents/upload")
                        .file(textFile("../../evil.txt", "safe"))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.originalFilename").value("evil.txt"))
                .andReturn();

        long docId = extractLong(result.getResponse().getContentAsString(), "id");
        Document document = documentRepository.findById(docId).orElseThrow();
        Path uploadRoot = fileStorageService.getUploadRoot();
        Path storedPath = Path.of(document.getFilePath()).normalize();

        assertThat(document.getStoredFilename()).doesNotContain("..", "/", "\\");
        assertThat(storedPath.startsWith(uploadRoot)).isTrue();
        assertThat(Files.exists(storedPath)).isTrue();
        assertThat(Files.notExists(uploadRoot.getParent().resolve("evil.txt"))).isTrue();
    }

    @Test
    void shouldUpdateOnlyCurrentUserDocument() throws Exception {
        String tokenA = registerAndLogin("alice");
        String tokenB = registerAndLogin("bob");
        long docA = createDocument(tokenA, "Alice Document");
        createDocument(tokenB, "Bob Document");

        mockMvc.perform(put("/api/documents/{id}", docA)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Alice Updated",
                                  "description": "updated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Alice Updated"))
                .andExpect(jsonPath("$.data.description").value("updated"));
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

    private long uploadDocument(String token, String filename, String content) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/documents/upload")
                        .file(textFile(filename, content))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        return extractLong(result.getResponse().getContentAsString(), "id");
    }

    private MockMultipartFile textFile(String filename, String content) {
        return new MockMultipartFile("file", filename, MediaType.TEXT_PLAIN_VALUE,
                content.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile markdownFile(String filename, String content) {
        return new MockMultipartFile("file", filename, "text/markdown",
                content.getBytes(StandardCharsets.UTF_8));
    }

    private String moduleFiveSmokeText() {
        return """
                这是 Secure Vault AI 模块五冒烟测试文档。

                本文件用于验证：
                1. 上传后自动解析。
                2. 解析后自动分块。
                3. chunkCount 大于 0。
                4. chunks 接口可以返回内容。
                5. text 接口仍然可以返回完整 extractedText。
                6. 列表和详情接口不能暴露 extractedText 和 filePath。
                7. 用户隔离必须生效。

                Spring Security、JWT、Document Chunking、User Isolation。
                这是第二段文本，用来帮助分块逻辑识别段落边界。
                这是第三段文本，用来验证中文内容不会乱码。
                """;
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
