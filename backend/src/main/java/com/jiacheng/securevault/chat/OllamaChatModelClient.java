package com.jiacheng.securevault.chat;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.chat", name = "provider", havingValue = "ollama")
public class OllamaChatModelClient implements ChatModelClient {

    private static final String SAFE_FAILURE = "本地问答模型暂时不可用，请检查 Ollama 是否启动以及模型是否已安装。";

    private final ChatProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaChatModelClient(ChatProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getOllama().getTimeoutSeconds()))
                .build();
    }

    @Override
    public ChatCompletion complete(ChatCompletionRequest request) {
        try {
            String body = objectMapper.writeValueAsString(requestBody(request));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint()))
                    .timeout(Duration.ofSeconds(properties.getOllama().getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return safeCompletion();
            }
            String answer = extractAnswer(response.body());
            if (!StringUtils.hasText(answer)) {
                return safeCompletion();
            }
            return new ChatCompletion(answer.trim(), providerName(), modelName());
        } catch (Exception ex) {
            return safeCompletion();
        }
    }

    @Override
    public String providerName() {
        return "ollama";
    }

    @Override
    public String modelName() {
        return properties.getOllama().getModel();
    }

    private ChatCompletion safeCompletion() {
        return new ChatCompletion(SAFE_FAILURE, providerName(), modelName());
    }

    private Map<String, Object> requestBody(ChatCompletionRequest request) {
        return Map.of(
                "model", properties.getOllama().getModel(),
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", request.getSystemPrompt()),
                        Map.of("role", "user", "content", request.getUserPrompt())
                )
        );
    }

    private String endpoint() {
        return stripTrailingSlash(properties.getOllama().getBaseUrl()) + "/api/chat";
    }

    private String stripTrailingSlash(String value) {
        String normalized = value == null || value.isBlank() ? "http://localhost:11434" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    @SuppressWarnings("unchecked")
    private String extractAnswer(String body) {
        try {
            Map<String, Object> response = objectMapper.readValue(body, Map.class);
            Object message = response.get("message");
            if (message instanceof Map<?, ?> messageMap) {
                Object content = messageMap.get("content");
                return content == null ? null : content.toString();
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }
}
