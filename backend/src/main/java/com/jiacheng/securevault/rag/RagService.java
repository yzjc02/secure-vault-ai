package com.jiacheng.securevault.rag;

import com.jiacheng.securevault.chat.AskRequest;
import com.jiacheng.securevault.chat.AskResponse;
import com.jiacheng.securevault.chat.ChatCompletion;
import com.jiacheng.securevault.chat.ChatCompletionRequest;
import com.jiacheng.securevault.chat.ChatModelClient;
import com.jiacheng.securevault.chat.DeterministicChatModelClient;
import com.jiacheng.securevault.conversation.entity.ChatMessage;
import com.jiacheng.securevault.conversation.entity.ChatRole;
import com.jiacheng.securevault.conversation.entity.Conversation;
import com.jiacheng.securevault.conversation.service.ConversationService;
import com.jiacheng.securevault.document.dto.SemanticSearchRequest;
import com.jiacheng.securevault.document.dto.SimilarChunkResponse;
import com.jiacheng.securevault.document.service.DocumentEmbeddingService;
import com.jiacheng.securevault.exception.BusinessException;
import com.jiacheng.securevault.security.AccessControlService;
import com.jiacheng.securevault.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class RagService {

    private static final String DOCUMENT_NOT_FOUND = "Document not found";

    private final CurrentUserService currentUserService;
    private final AccessControlService accessControlService;
    private final DocumentEmbeddingService documentEmbeddingService;
    private final ConversationService conversationService;
    private final RagPromptBuilder promptBuilder;
    private final RagProperties ragProperties;
    private final ChatModelClient chatModelClient;

    public RagService(CurrentUserService currentUserService,
                      AccessControlService accessControlService,
                      DocumentEmbeddingService documentEmbeddingService,
                      ConversationService conversationService,
                      RagPromptBuilder promptBuilder,
                      RagProperties ragProperties,
                      ChatModelClient chatModelClient) {
        this.currentUserService = currentUserService;
        this.accessControlService = accessControlService;
        this.documentEmbeddingService = documentEmbeddingService;
        this.conversationService = conversationService;
        this.promptBuilder = promptBuilder;
        this.ragProperties = ragProperties;
        this.chatModelClient = chatModelClient;
    }

    @Transactional
    public AskResponse ask(AskRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        String question = normalizeQuestion(request.getQuestion());
        int topK = normalizeTopK(request.getTopK());
        Long documentId = request.getDocumentId();
        if (documentId != null) {
            accessControlService.requireOwnedDocument(documentId, currentUserId);
        }
        Conversation conversation = request.getConversationId() == null
                ? conversationService.createConversation(currentUserId, question)
                : conversationService.getOwnedConversation(request.getConversationId(), currentUserId);

        List<RagSource> sources = searchSources(question, documentId, topK);
        List<RagSourceResponse> sourceResponses = sources.stream()
                .map(RagSource::toResponse)
                .toList();

        ChatCompletion completion = sources.isEmpty()
                ? new ChatCompletion(DeterministicChatModelClient.NO_SOURCES_ANSWER,
                        chatModelClient.providerName(),
                        chatModelClient.modelName())
                : complete(question, sourceResponses, sources);

        ChatMessage userMessage = conversationService.saveMessage(
                conversation.getId(),
                currentUserId,
                ChatRole.USER,
                question,
                List.of()
        );
        ChatMessage assistantMessage = conversationService.saveMessage(
                conversation.getId(),
                currentUserId,
                ChatRole.ASSISTANT,
                completion.getAnswer(),
                sourceResponses
        );

        return new AskResponse(
                conversation.getId(),
                userMessage.getId(),
                assistantMessage.getId(),
                completion.getAnswer(),
                sourceResponses,
                completion.getModel(),
                completion.getProvider(),
                topK
        );
    }

    private ChatCompletion complete(String question, List<RagSourceResponse> sourceResponses, List<RagSource> sources) {
        ChatCompletionRequest completionRequest = new ChatCompletionRequest(
                promptBuilder.systemPrompt(),
                promptBuilder.buildUserPrompt(question, sources),
                question,
                sourceResponses
        );
        return chatModelClient.complete(completionRequest);
    }

    private List<RagSource> searchSources(String question, Long documentId, int topK) {
        SemanticSearchRequest searchRequest = new SemanticSearchRequest();
        searchRequest.setQuery(question);
        searchRequest.setDocumentId(documentId);
        searchRequest.setTopK(topK);
        List<SimilarChunkResponse> chunks = documentEmbeddingService.searchSimilarChunks(searchRequest);
        return mapSources(chunks);
    }

    private List<RagSource> mapSources(List<SimilarChunkResponse> chunks) {
        int previewLength = ragProperties.getSourcePreviewLength();
        return java.util.stream.IntStream.range(0, chunks.size())
                .mapToObj(index -> RagSource.from(chunks.get(index), index + 1, previewLength))
                .toList();
    }

    private String normalizeQuestion(String question) {
        if (!StringUtils.hasText(question)) {
            throw new BusinessException(400, "question must not be blank");
        }
        String normalized = question.trim();
        if (normalized.length() > ragProperties.getMaxQuestionLength()) {
            throw new BusinessException(400, "question length must be at most " + ragProperties.getMaxQuestionLength());
        }
        return normalized;
    }

    private int normalizeTopK(Integer topK) {
        int resolved = topK == null ? ragProperties.getDefaultTopK() : topK;
        if (resolved < 1 || resolved > ragProperties.getMaxTopK()) {
            throw new BusinessException(400, "topK must be between 1 and " + ragProperties.getMaxTopK());
        }
        return resolved;
    }
}
