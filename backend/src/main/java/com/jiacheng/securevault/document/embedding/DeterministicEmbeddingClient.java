package com.jiacheng.securevault.document.embedding;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.embedding", name = "provider", havingValue = "deterministic", matchIfMissing = true)
public class DeterministicEmbeddingClient implements EmbeddingClient {

    private final EmbeddingProperties properties;

    public DeterministicEmbeddingClient(EmbeddingProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<Double> embed(String text) {
        if (!StringUtils.hasText(text)) {
            throw new EmbeddingException("Embedding text is empty");
        }
        int dimension = properties.getDimension();
        List<Double> values = new ArrayList<>(dimension);
        String normalizedText = text.trim();
        for (int i = 0; i < dimension; i++) {
            byte[] hash = EmbeddingVectorUtils.sha256(normalizedText + "#" + i);
            int first = hash[0] & 0xff;
            int second = hash[1] & 0xff;
            double value = ((first * 256 + second) / 32767.5d) - 1.0d;
            values.add(value);
        }
        List<Double> normalized = EmbeddingVectorUtils.normalize(values);
        EmbeddingVectorUtils.validate(normalized, dimension);
        return normalized;
    }

    @Override
    public String model() {
        return properties.getModel();
    }

    @Override
    public int dimension() {
        return properties.getDimension();
    }
}
