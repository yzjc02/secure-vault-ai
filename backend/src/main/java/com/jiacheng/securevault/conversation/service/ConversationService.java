package com.jiacheng.securevault.conversation.service;

import com.jiacheng.securevault.conversation.dto.ChatMessageResponse;
import com.jiacheng.securevault.conversation.dto.ConversationMessagesResponse;
import com.jiacheng.securevault.conversation.dto.ConversationResponse;
import com.jiacheng.securevault.conversation.entity.ChatMessage;
import com.jiacheng.securevault.conversation.entity.ChatRole;
import com.jiacheng.securevault.conversation.entity.Conversation;
import com.jiacheng.securevault.conversation.repository.ChatMessageRepository;
import com.jiacheng.securevault.conversation.repository.ConversationRepository;
import com.jiacheng.securevault.exception.BusinessException;
import com.jiacheng.securevault.rag.RagSourceResponse;
import com.jiacheng.securevault.security.AccessControlService;
import com.jiacheng.securevault.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class ConversationService {

    private static final String CONVERSATION_NOT_FOUND = "Conversation not found";
    private static final int MAX_TITLE_LENGTH = 50;

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CurrentUserService currentUserService;
    private final AccessControlService accessControlService;
    private final ObjectMapper objectMapper;

    public ConversationService(ConversationRepository conversationRepository,
                               ChatMessageRepository chatMessageRepository,
                               CurrentUserService currentUserService,
                               AccessControlService accessControlService,
                               ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.currentUserService = currentUserService;
        this.accessControlService = accessControlService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listCurrentUserConversations() {
        Long currentUserId = currentUserService.getCurrentUserId();
        return conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(currentUserId)
                .stream()
                .map(conversation -> ConversationResponse.from(
                        conversation,
                        chatMessageRepository.countByConversationIdAndUserId(conversation.getId(), currentUserId)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationMessagesResponse getCurrentUserMessages(Long conversationId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Conversation conversation = getOwnedConversation(conversationId, currentUserId);
        List<ChatMessageResponse> messages = chatMessageRepository
                .findAllByConversationIdAndUserIdOrderByCreatedAtAsc(conversationId, currentUserId)
                .stream()
                .map(message -> ChatMessageResponse.from(message, readSources(message.getSourcesJson())))
                .toList();
        return new ConversationMessagesResponse(conversation.getId(), conversation.getTitle(), messages);
    }

    @Transactional(readOnly = true)
    public Conversation getOwnedConversation(Long conversationId, Long userId) {
        return accessControlService.requireOwnedConversation(conversationId, userId);
    }

    @Transactional
    public Conversation createConversation(Long userId, String question) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(buildTitle(question));
        return conversationRepository.save(conversation);
    }

    @Transactional
    public ChatMessage saveMessage(Long conversationId,
                                   Long userId,
                                   ChatRole role,
                                   String content,
                                   List<RagSourceResponse> sources) {
        getOwnedConversation(conversationId, userId);
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setSourcesJson(writeSources(sources));
        ChatMessage saved = chatMessageRepository.save(message);
        touchConversation(conversationId, userId);
        return saved;
    }

    public String writeSources(List<RagSourceResponse> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (Exception ex) {
            throw new BusinessException(500, "Failed to serialize chat sources");
        }
    }

    public List<RagSourceResponse> readSources(String sourcesJson) {
        if (!StringUtils.hasText(sourcesJson)) {
            return List.of();
        }
        try {
            RagSourceResponse[] sources = objectMapper.readValue(sourcesJson, RagSourceResponse[].class);
            return Arrays.asList(sources);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void touchConversation(Long conversationId, Long userId) {
        Conversation conversation = getOwnedConversation(conversationId, userId);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    private String buildTitle(String question) {
        String normalized = StringUtils.hasText(question) ? question.trim() : "New conversation";
        if (normalized.length() <= MAX_TITLE_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_TITLE_LENGTH);
    }
}
