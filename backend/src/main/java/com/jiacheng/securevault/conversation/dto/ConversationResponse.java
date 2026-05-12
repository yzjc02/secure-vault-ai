package com.jiacheng.securevault.conversation.dto;

import com.jiacheng.securevault.conversation.entity.Conversation;

import java.time.LocalDateTime;

public class ConversationResponse {

    private Long conversationId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long messageCount;

    public ConversationResponse(Long conversationId,
                                String title,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt,
                                Long messageCount) {
        this.conversationId = conversationId;
        this.title = title;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.messageCount = messageCount;
    }

    public static ConversationResponse from(Conversation conversation, long messageCount) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messageCount
        );
    }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getMessageCount() { return messageCount; }
    public void setMessageCount(Long messageCount) { this.messageCount = messageCount; }
}
