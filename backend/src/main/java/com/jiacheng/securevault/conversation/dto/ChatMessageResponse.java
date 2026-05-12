package com.jiacheng.securevault.conversation.dto;

import com.jiacheng.securevault.conversation.entity.ChatMessage;
import com.jiacheng.securevault.rag.RagSourceResponse;

import java.time.LocalDateTime;
import java.util.List;

public class ChatMessageResponse {

    private Long messageId;
    private String role;
    private String content;
    private List<RagSourceResponse> sources;
    private LocalDateTime createdAt;

    public ChatMessageResponse(Long messageId,
                               String role,
                               String content,
                               List<RagSourceResponse> sources,
                               LocalDateTime createdAt) {
        this.messageId = messageId;
        this.role = role;
        this.content = content;
        this.sources = sources;
        this.createdAt = createdAt;
    }

    public static ChatMessageResponse from(ChatMessage message, List<RagSourceResponse> sources) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole().name(),
                message.getContent(),
                sources,
                message.getCreatedAt()
        );
    }

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<RagSourceResponse> getSources() { return sources; }
    public void setSources(List<RagSourceResponse> sources) { this.sources = sources; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
