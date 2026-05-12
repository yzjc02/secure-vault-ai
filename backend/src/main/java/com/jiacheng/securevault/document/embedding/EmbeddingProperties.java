package com.jiacheng.securevault.document.embedding;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.embedding")
public class EmbeddingProperties {

    private String provider = "deterministic";

    private String model = "nomic-embed-text";

    @Min(value = 1, message = "embedding dimension must be greater than 0")
    @Max(value = 8192, message = "embedding dimension must be at most 8192")
    private int dimension = 768;

    @Min(value = 1, message = "embedding topK must be at least 1")
    @Max(value = 20, message = "embedding topK must be at most 20")
    private int topK = 5;

    @Min(value = 1, message = "embedding timeoutSeconds must be greater than 0")
    @Max(value = 300, message = "embedding timeoutSeconds must be at most 300")
    private int timeoutSeconds = 30;

    @Min(value = 1, message = "embedding batchSize must be greater than 0")
    @Max(value = 100, message = "embedding batchSize must be at most 100")
    private int batchSize = 16;

    private Ollama ollama = new Ollama();

    @AssertTrue(message = "embedding provider must be deterministic or ollama")
    public boolean isProviderValid() {
        return "deterministic".equalsIgnoreCase(provider) || "ollama".equalsIgnoreCase(provider);
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getDimension() { return dimension; }
    public void setDimension(int dimension) { this.dimension = dimension; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public Ollama getOllama() { return ollama; }
    public void setOllama(Ollama ollama) { this.ollama = ollama; }

    public static class Ollama {
        private String baseUrl = "http://localhost:11434";
        private String embeddingsPath = "/api/embeddings";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getEmbeddingsPath() { return embeddingsPath; }
        public void setEmbeddingsPath(String embeddingsPath) { this.embeddingsPath = embeddingsPath; }
    }
}
