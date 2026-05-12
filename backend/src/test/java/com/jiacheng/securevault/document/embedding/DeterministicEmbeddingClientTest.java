package com.jiacheng.securevault.document.embedding;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicEmbeddingClientTest {

    @Test
    void shouldReturnConfiguredDimension() {
        DeterministicEmbeddingClient client = client(12);

        assertThat(client.embed("hello")).hasSize(12);
    }

    @Test
    void shouldReturnSameVectorForSameText() {
        DeterministicEmbeddingClient client = client(16);

        assertThat(client.embed("same text")).isEqualTo(client.embed("same text"));
    }

    @Test
    void shouldReturnDifferentVectorForDifferentText() {
        DeterministicEmbeddingClient client = client(16);

        assertThat(client.embed("first")).isNotEqualTo(client.embed("second"));
    }

    @Test
    void shouldNotReturnInvalidNumbers() {
        DeterministicEmbeddingClient client = client(32);

        List<Double> vector = client.embed("valid text");

        assertThat(vector).allSatisfy(value -> assertThat(Double.isFinite(value)).isTrue());
    }

    private DeterministicEmbeddingClient client(int dimension) {
        EmbeddingProperties properties = new EmbeddingProperties();
        properties.setDimension(dimension);
        return new DeterministicEmbeddingClient(properties);
    }
}
