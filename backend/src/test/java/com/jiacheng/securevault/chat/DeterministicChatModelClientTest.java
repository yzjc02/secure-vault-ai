package com.jiacheng.securevault.chat;

import com.jiacheng.securevault.rag.RagSourceResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicChatModelClientTest {

    private final DeterministicChatModelClient client = new DeterministicChatModelClient();

    @Test
    void shouldReturnStableNonEmptyAnswerWithSources() {
        ChatCompletionRequest request = request(List.of(source("S1")));

        ChatCompletion first = client.complete(request);
        ChatCompletion second = client.complete(request);

        assertThat(first.getAnswer()).isNotBlank();
        assertThat(first.getAnswer()).isEqualTo(second.getAnswer());
        assertThat(first.getAnswer()).contains("[S1]");
    }

    @Test
    void shouldReturnFallbackWithoutSources() {
        ChatCompletion completion = client.complete(request(List.of()));

        assertThat(completion.getAnswer()).isEqualTo(DeterministicChatModelClient.NO_SOURCES_ANSWER);
    }

    @Test
    void shouldReturnProviderAndModelNames() {
        assertThat(client.providerName()).isEqualTo("deterministic");
        assertThat(client.modelName()).isEqualTo("deterministic");
    }

    private ChatCompletionRequest request(List<RagSourceResponse> sources) {
        return new ChatCompletionRequest("system", "user", "What does module seven validate?", sources);
    }

    private RagSourceResponse source(String sourceId) {
        return new RagSourceResponse(
                sourceId,
                100L,
                10L,
                "Module Seven",
                "module7.txt",
                0,
                0.9d,
                "preview",
                LocalDateTime.now()
        );
    }
}
