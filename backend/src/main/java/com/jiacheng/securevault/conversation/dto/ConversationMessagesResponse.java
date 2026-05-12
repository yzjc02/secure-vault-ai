package com.jiacheng.securevault.conversation.dto;

import java.util.List;

public class ConversationMessagesResponse {

    private Long conversationId;
    private String title;
    private List<ChatMessageResponse> messages;

    public ConversationMessagesResponse(Long conversationId, String title, List<ChatMessageResponse> messages) {
        this.conversationId = conversationId;
        this.title = title;
        this.messages = messages;
    }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<ChatMessageResponse> getMessages() { return messages; }
    public void setMessages(List<ChatMessageResponse> messages) { this.messages = messages; }
}
