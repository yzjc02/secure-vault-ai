package com.jiacheng.securevault.chat;

import com.jiacheng.securevault.common.ApiResponse;
import com.jiacheng.securevault.rag.RagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public ApiResponse<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        return ApiResponse.success(ragService.ask(request));
    }
}
