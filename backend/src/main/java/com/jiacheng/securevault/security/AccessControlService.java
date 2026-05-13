package com.jiacheng.securevault.security;

import com.jiacheng.securevault.audit.enums.AuditAction;
import com.jiacheng.securevault.audit.enums.AuditResourceType;
import com.jiacheng.securevault.audit.service.AuditLogService;
import com.jiacheng.securevault.conversation.entity.Conversation;
import com.jiacheng.securevault.conversation.repository.ConversationRepository;
import com.jiacheng.securevault.document.entity.Document;
import com.jiacheng.securevault.document.entity.DocumentChunk;
import com.jiacheng.securevault.document.repository.DocumentChunkRepository;
import com.jiacheng.securevault.document.repository.DocumentRepository;
import com.jiacheng.securevault.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class AccessControlService {

    private static final String DOCUMENT_NOT_FOUND = "Document not found";
    private static final String CONVERSATION_NOT_FOUND = "Conversation not found";
    private static final String CHUNK_NOT_FOUND = "Chunk not found";

    private final CurrentUserService currentUserService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ConversationRepository conversationRepository;
    private final AuditLogService auditLogService;

    public AccessControlService(CurrentUserService currentUserService,
                                DocumentRepository documentRepository,
                                DocumentChunkRepository documentChunkRepository,
                                ConversationRepository conversationRepository,
                                AuditLogService auditLogService) {
        this.currentUserService = currentUserService;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.conversationRepository = conversationRepository;
        this.auditLogService = auditLogService;
    }

    public Long currentUserId() {
        return currentUserService.getCurrentUserId();
    }

    public Document requireOwnedDocument(Long documentId) {
        return requireOwnedDocument(documentId, currentUserId());
    }

    public Document requireOwnedDocument(Long documentId, Long currentUserId) {
        return documentRepository.findByIdAndUserId(documentId, currentUserId)
                .orElseThrow(() -> {
                    recordAccessDenied(currentUserId, AuditResourceType.DOCUMENT, documentId);
                    return new BusinessException(404, DOCUMENT_NOT_FOUND);
                });
    }

    public Conversation requireOwnedConversation(Long conversationId) {
        return requireOwnedConversation(conversationId, currentUserId());
    }

    public Conversation requireOwnedConversation(Long conversationId, Long currentUserId) {
        return conversationRepository.findByIdAndUserId(conversationId, currentUserId)
                .orElseThrow(() -> {
                    recordAccessDenied(currentUserId, AuditResourceType.CONVERSATION, conversationId);
                    return new BusinessException(404, CONVERSATION_NOT_FOUND);
                });
    }

    public DocumentChunk requireOwnedChunk(Long chunkId) {
        Long currentUserId = currentUserId();
        return documentChunkRepository.findByIdAndUserId(chunkId, currentUserId)
                .orElseThrow(() -> {
                    recordAccessDenied(currentUserId, AuditResourceType.DOCUMENT_CHUNK, chunkId);
                    return new BusinessException(404, CHUNK_NOT_FOUND);
                });
    }

    public void requireOwnedDocumentForChunk(Long documentId, Long currentUserId) {
        requireOwnedDocument(documentId, currentUserId);
    }

    private void recordAccessDenied(Long currentUserId, AuditResourceType resourceType, Long resourceId) {
        auditLogService.recordForUser(currentUserId, AuditAction.RESOURCE_ACCESS_DENIED,
                resourceType, resourceId, false, "Resource not found or not accessible");
    }
}
