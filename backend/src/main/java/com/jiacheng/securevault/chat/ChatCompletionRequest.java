package com.jiacheng.securevault.chat;

import com.jiacheng.securevault.rag.RagSourceResponse;

import java.util.List;

public class ChatCompletionRequest {

    private final String systemPrompt;
    private final String userPrompt;
    private final String question;
    private final List<RagSourceResponse> sources;

    public ChatCompletionRequest(String systemPrompt,
                                 String userPrompt,
                                 String question,
                                 List<RagSourceResponse> sources) {
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
        this.question = question;
        this.sources = sources == null ? List.of() : List.copyOf(sources);
    }

    public String getSystemPrompt() { return systemPrompt; }
    public String getUserPrompt() { return userPrompt; }
    public String getQuestion() { return question; }
    public List<RagSourceResponse> getSources() { return sources; }
}
