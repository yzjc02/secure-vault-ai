package com.jiacheng.securevault.conversation.controller;

import com.jiacheng.securevault.common.ApiResponse;
import com.jiacheng.securevault.conversation.dto.ConversationMessagesResponse;
import com.jiacheng.securevault.conversation.dto.ConversationResponse;
import com.jiacheng.securevault.conversation.service.ConversationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public ApiResponse<List<ConversationResponse>> list() {
        return ApiResponse.success(conversationService.listCurrentUserConversations());
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<ConversationMessagesResponse> messages(@PathVariable Long id) {
        return ApiResponse.success(conversationService.getCurrentUserMessages(id));
    }
}
