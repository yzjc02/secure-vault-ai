package com.jiacheng.securevault.chat;

import jakarta.validation.constraints.NotBlank;

public class AskRequest {

    @NotBlank(message = "question must not be blank")
    private String question;

    private Integer topK;

    private Long documentId;

    private Long conversationId;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
}
