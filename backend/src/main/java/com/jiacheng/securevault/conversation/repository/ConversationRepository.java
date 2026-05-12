package com.jiacheng.securevault.conversation.repository;

import com.jiacheng.securevault.conversation.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<Conversation> findByIdAndUserId(Long id, Long userId);
}
