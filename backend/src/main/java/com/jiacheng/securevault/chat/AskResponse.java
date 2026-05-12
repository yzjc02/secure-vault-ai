package com.jiacheng.securevault.chat;

import com.jiacheng.securevault.rag.RagSourceResponse;

import java.util.List;

public class AskResponse {

    private Long conversationId;
    private Long userMessageId;
    private Long assistantMessageId;
    private String answer;
    private List<RagSourceResponse> sources;
    private String model;
    private String provider;
    private Integer usedTopK;

    public AskResponse(Long conversationId,
                       Long userMessageId,
                       Long assistantMessageId,
                       String answer,
                       List<RagSourceResponse> sources,
                       String model,
                       String provider,
                       Integer usedTopK) {
        this.conversationId = conversationId;
        this.userMessageId = userMessageId;
        this.assistantMessageId = assistantMessageId;
        this.answer = answer;
        this.sources = sources;
        this.model = model;
        this.provider = provider;
        this.usedTopK = usedTopK;
    }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public Long getUserMessageId() { return userMessageId; }
    public void setUserMessageId(Long userMessageId) { this.userMessageId = userMessageId; }
    public Long getAssistantMessageId() { return assistantMessageId; }
    public void setAssistantMessageId(Long assistantMessageId) { this.assistantMessageId = assistantMessageId; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public List<RagSourceResponse> getSources() { return sources; }
    public void setSources(List<RagSourceResponse> sources) { this.sources = sources; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Integer getUsedTopK() { return usedTopK; }
    public void setUsedTopK(Integer usedTopK) { this.usedTopK = usedTopK; }
}
