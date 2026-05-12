package com.jiacheng.securevault.rag;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagPromptBuilderTest {

    @Test
    void shouldBuildPromptWithQuestionSourceIdAndContent() {
        RagPromptBuilder builder = new RagPromptBuilder(properties(8000));
        RagSource source = source("S1", "chunk content about module seven");

        String prompt = builder.buildPrompt("What does module seven validate?", List.of(source));

        assertThat(prompt).isNotNull();
        assertThat(prompt).contains("What does module seven validate?");
        assertThat(prompt).contains("[S1]");
        assertThat(prompt).contains("chunk content about module seven");
    }

    @Test
    void shouldTruncateOverlongContext() {
        RagPromptBuilder builder = new RagPromptBuilder(properties(180));
        String longContent = "A".repeat(1000);

        String prompt = builder.buildPrompt("question", List.of(source("S1", longContent)));

        assertThat(prompt).isNotNull();
        assertThat(prompt).contains("[S1]");
        assertThat(prompt).doesNotContain("A".repeat(500));
        assertThat(prompt.length()).isLessThan(600);
    }

    private RagProperties properties(int maxContextChars) {
        RagProperties properties = new RagProperties();
        properties.setMaxContextChars(maxContextChars);
        return properties;
    }

    private RagSource source(String sourceId, String content) {
        return new RagSource(
                sourceId,
                100L,
                10L,
                "Module Seven",
                "module7.txt",
                0,
                0.87d,
                content,
                content,
                "deterministic",
                LocalDateTime.now()
        );
    }
}
