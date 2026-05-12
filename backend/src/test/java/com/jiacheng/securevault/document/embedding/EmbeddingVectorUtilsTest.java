package com.jiacheng.securevault.document.embedding;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddingVectorUtilsTest {

    @Test
    void shouldRejectWrongDimension() {
        assertThatThrownBy(() -> EmbeddingVectorUtils.validate(List.of(0.1d, 0.2d), 3))
                .isInstanceOf(EmbeddingException.class)
                .hasMessage("Embedding dimension mismatch");
    }

    @Test
    void shouldRejectInvalidNumbers() {
        assertThatThrownBy(() -> EmbeddingVectorUtils.validate(List.of(0.1d, Double.NaN), 2))
                .isInstanceOf(EmbeddingException.class);
        assertThatThrownBy(() -> EmbeddingVectorUtils.validate(List.of(0.1d, Double.POSITIVE_INFINITY), 2))
                .isInstanceOf(EmbeddingException.class);
    }

    @Test
    void shouldSerializePgvectorString() {
        String vector = EmbeddingVectorUtils.toPgvectorString(List.of(0.1d, -0.2d), 2);

        assertThat(vector).isEqualTo("[0.1000000000,-0.2000000000]");
    }

    @Test
    void shouldCalculateStableCosineSimilarity() {
        double score = EmbeddingVectorUtils.cosineSimilarity(List.of(1.0d, 0.0d), List.of(1.0d, 0.0d));

        assertThat(score).isEqualTo(1.0d);
    }

    @Test
    void shouldParseJsonVector() {
        List<Double> vector = EmbeddingVectorUtils.parseJson("[0.1,-0.2]");

        assertThat(vector).containsExactly(0.1d, -0.2d);
    }
}
