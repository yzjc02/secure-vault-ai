package com.jiacheng.securevault.document;

import com.jiacheng.securevault.document.service.ChunkingProperties;
import com.jiacheng.securevault.document.service.TextChunkingService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkingServiceTest {

    @Test
    void shouldReturnEmptyListWhenTextIsBlank() {
        TextChunkingService service = service(30, 5, 8);

        assertThat(service.split(null)).isEmpty();
        assertThat(service.split("   \n\n  ")).isEmpty();
    }

    @Test
    void shouldCreateSingleChunkForShortText() {
        TextChunkingService service = service(30, 5, 8);

        List<TextChunkingService.ChunkCandidate> chunks = service.split(" hello world ");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).chunkIndex()).isZero();
        assertThat(chunks.get(0).content()).isEqualTo("hello world");
        assertThat(chunks.get(0).contentLength()).isEqualTo(11);
        assertThat(chunks.get(0).contentHash()).hasSize(64);
    }

    @Test
    void shouldCreateMultipleContinuousChunksForLongText() {
        TextChunkingService service = service(30, 5, 8);

        List<TextChunkingService.ChunkCandidate> chunks = service.split("abcdefghij".repeat(10));

        assertThat(chunks).hasSizeGreaterThan(1);
        for (int i = 0; i < chunks.size(); i++) {
            TextChunkingService.ChunkCandidate chunk = chunks.get(i);
            assertThat(chunk.chunkIndex()).isEqualTo(i);
            assertThat(chunk.content()).isNotBlank();
            assertThat(chunk.contentLength()).isEqualTo(chunk.content().length());
            assertThat(chunk.contentHash()).isNotBlank();
            assertThat(chunk.contentHash()).hasSize(64);
        }
    }

    @Test
    void shouldKeepHashStable() {
        TextChunkingService service = service(30, 5, 8);

        String firstHash = service.split("same content").get(0).contentHash();
        String secondHash = service.split("same content").get(0).contentHash();

        assertThat(firstHash).isEqualTo(secondHash);
    }

    @Test
    void shouldApplyOverlapBetweenChunks() {
        TextChunkingService service = service(30, 5, 8);

        List<TextChunkingService.ChunkCandidate> chunks = service.split("abcdefghij".repeat(8));

        assertThat(chunks).hasSizeGreaterThan(1);
        for (int i = 1; i < chunks.size(); i++) {
            assertThat(chunks.get(i).startOffset()).isLessThan(chunks.get(i - 1).endOffset());
        }
    }

    @Test
    void shouldPreferChineseSentenceBoundaryNearWindowEnd() {
        TextChunkingService service = service(12, 2, 4);

        List<TextChunkingService.ChunkCandidate> chunks = service.split("一二三四五六七八九十。后续内容后续内容后续内容");

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.get(0).content()).endsWith("。");
    }

    @Test
    void shouldPreferEnglishSentenceBoundaryNearWindowEnd() {
        TextChunkingService service = service(18, 3, 6);

        List<TextChunkingService.ChunkCandidate> chunks = service.split("This is sentence. Next sentence keeps going.");

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.get(0).content()).endsWith(".");
    }

    @Test
    void shouldNormalizeLineBreaksAndCompressBlankLines() {
        TextChunkingService service = service(100, 10, 10);

        String cleaned = service.clean("A  \r\n\r\n\r\n\r\nB\t \rC\u0000");

        assertThat(cleaned).isEqualTo("A\n\nB\nC");
    }

    @Test
    void shouldNotDuplicateChineseCharactersForDefaultSingleChunkSmokeText() {
        TextChunkingService service = service(1000, 150, 100);
        String text = moduleFiveSmokeText();
        String cleaned = service.clean(text);

        List<TextChunkingService.ChunkCandidate> chunks = service.split(text);

        assertThat(chunks).hasSize(1);
        TextChunkingService.ChunkCandidate chunk = chunks.get(0);
        assertThat(chunk.content()).isEqualTo(cleaned);
        assertThat(chunk.startOffset()).isZero();
        assertThat(chunk.endOffset()).isEqualTo(cleaned.length());
        assertThat(chunk.content()).contains("chunks 接口可以返回内容。");
        assertThat(chunk.content()).contains("text 接口仍然可以返回完整 extractedText。");
        assertThat(chunk.content()).contains("列表和详情接口不能暴露 extractedText 和 filePath。");
        assertThat(chunk.content()).contains("用户隔离必须生效。");
        assertThat(chunk.content()).doesNotContain("接接口口");
        assertThat(chunk.content()).doesNotContain("可可以以");
        assertThat(chunk.content()).doesNotContain("返返回回");
        assertThat(chunk.content()).doesNotContain("内内容容");
        assertThat(chunk.content()).doesNotContain("列列表表");
        assertThat(chunk.content()).doesNotContain("详详情情");
        assertThat(chunk.content()).doesNotContain("用用户户");
        assertThat(chunk.content()).doesNotContain("生生效效");
    }

    @Test
    void shouldAlwaysMakeProgressWithLargeOverlap() {
        TextChunkingService service = service(30, 29, 8);

        List<TextChunkingService.ChunkCandidate> chunks = service.split("a".repeat(300));

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.size()).isLessThan(300);
        for (int i = 1; i < chunks.size(); i++) {
            assertThat(chunks.get(i).startOffset()).isGreaterThan(chunks.get(i - 1).startOffset());
        }
    }

    @Test
    void shouldMergeSmallTailIntoPreviousChunk() {
        TextChunkingService service = service(20, 0, 8);

        List<TextChunkingService.ChunkCandidate> chunks = service.split("a".repeat(45));

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(chunks.size() - 1).contentLength()).isEqualTo(25);
    }

    private TextChunkingService service(int chunkSize, int overlapSize, int minChunkSize) {
        ChunkingProperties properties = new ChunkingProperties();
        properties.setChunkSize(chunkSize);
        properties.setOverlapSize(overlapSize);
        properties.setMinChunkSize(minChunkSize);
        return new TextChunkingService(properties);
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
}
