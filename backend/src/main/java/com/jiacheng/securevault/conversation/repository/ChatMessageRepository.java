package com.jiacheng.securevault.conversation.repository;

import com.jiacheng.securevault.conversation.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllByConversationIdAndUserIdOrderByCreatedAtAsc(Long conversationId, Long userId);

    long countByConversationIdAndUserId(Long conversationId, Long userId);

    void deleteByConversationIdAndUserId(Long conversationId, Long userId);
}
