package com.jiacheng.securevault.chat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.chat")
public class ChatProperties {

    private String provider = "deterministic";

    private Ollama ollama = new Ollama();

    @AssertTrue(message = "chat provider must be deterministic or ollama")
    public boolean isProviderValid() {
        return "deterministic".equalsIgnoreCase(provider) || "ollama".equalsIgnoreCase(provider);
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Ollama getOllama() { return ollama; }
    public void setOllama(Ollama ollama) { this.ollama = ollama; }

    public static class Ollama {
        private String baseUrl = "http://localhost:11434";
        private String model = "qwen2.5:3b";

        @Min(value = 1, message = "ollama chat timeoutSeconds must be greater than 0")
        @Max(value = 300, message = "ollama chat timeoutSeconds must be at most 300")
        private int timeoutSeconds = 60;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }
}
