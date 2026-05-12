package com.jiacheng.securevault.chat;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "app.chat", name = "provider", havingValue = "deterministic", matchIfMissing = true)
public class DeterministicChatModelClient implements ChatModelClient {

    public static final String NO_SOURCES_ANSWER =
            "我没有在你的知识库中找到足够相关的内容来回答这个问题。你可以先上传并向量化相关文档后再提问。";

    @Override
    public ChatCompletion complete(ChatCompletionRequest request) {
        if (request == null || request.getSources().isEmpty()) {
            return new ChatCompletion(NO_SOURCES_ANSWER, providerName(), modelName());
        }
        String firstSourceId = request.getSources().get(0).getSourceId();
        String question = request.getQuestion();
        String normalizedQuestion = StringUtils.hasText(question) ? question.trim() : "你的问题";
        String answer = "根据你的知识库片段 [" + firstSourceId + "]，可以回答："
                + normalizedQuestion
                + "。如需更精确结论，请结合 sources 中的片段预览继续核对。";
        return new ChatCompletion(answer, providerName(), modelName());
    }

    @Override
    public String providerName() {
        return "deterministic";
    }

    @Override
    public String modelName() {
        return "deterministic";
    }
}
