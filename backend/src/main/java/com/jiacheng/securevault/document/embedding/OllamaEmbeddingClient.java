package com.jiacheng.securevault.document.embedding;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.embedding", name = "provider", havingValue = "ollama")
public class OllamaEmbeddingClient implements EmbeddingClient {

    private static final String SAFE_FAILURE = "Embedding model request failed";

    private final EmbeddingProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaEmbeddingClient(EmbeddingProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    @Override
    public List<Double> embed(String text) {
        if (!StringUtils.hasText(text)) {
            throw new EmbeddingException("Embedding text is empty");
        }
        try {
            String body = objectMapper.writeValueAsString(requestBody(text.trim()));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint()))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new EmbeddingException(SAFE_FAILURE);
            }
            List<Double> embedding = extractEmbedding(response.body());
            EmbeddingVectorUtils.validate(embedding, properties.getDimension());
            return embedding;
        } catch (EmbeddingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new EmbeddingException(SAFE_FAILURE, ex);
        }
    }

    @Override
    public String model() {
        return properties.getModel();
    }

    @Override
    public int dimension() {
        return properties.getDimension();
    }

    private Map<String, Object> requestBody(String text) {
        String path = properties.getOllama().getEmbeddingsPath();
        if (path != null && path.endsWith("/api/embed")) {
            return Map.of("model", properties.getModel(), "input", text);
        }
        return Map.of("model", properties.getModel(), "prompt", text);
    }

    private String endpoint() {
        String baseUrl = stripTrailingSlash(properties.getOllama().getBaseUrl());
        String path = properties.getOllama().getEmbeddingsPath();
        if (path == null || path.isBlank()) {
            path = "/api/embeddings";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return baseUrl + path;
    }

    private String stripTrailingSlash(String value) {
        String normalized = value == null || value.isBlank() ? "http://localhost:11434" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    @SuppressWarnings("unchecked")
    private List<Double> extractEmbedding(String body) {
        try {
            Map<String, Object> response = objectMapper.readValue(body, Map.class);
            Object embedding = response.get("embedding");
            if (embedding instanceof List<?> values) {
                return toDoubleList(values);
            }
            Object embeddings = response.get("embeddings");
            if (embeddings instanceof List<?> outer && !outer.isEmpty() && outer.get(0) instanceof List<?> first) {
                return toDoubleList(first);
            }
            throw new EmbeddingException(SAFE_FAILURE);
        } catch (EmbeddingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new EmbeddingException(SAFE_FAILURE, ex);
        }
    }

    private List<Double> toDoubleList(List<?> values) {
        List<Double> result = new ArrayList<>(values.size());
        for (Object value : values) {
            if (!(value instanceof Number number)) {
                throw new EmbeddingException(SAFE_FAILURE);
            }
            result.add(number.doubleValue());
        }
        return result;
    }
}
