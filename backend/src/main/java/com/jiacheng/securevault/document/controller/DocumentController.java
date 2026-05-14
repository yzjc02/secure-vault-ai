package com.jiacheng.securevault.document.controller;

import com.jiacheng.securevault.common.ApiResponse;
import com.jiacheng.securevault.document.dto.DocumentCreateRequest;
import com.jiacheng.securevault.document.dto.DocumentChunkDetailResponse;
import com.jiacheng.securevault.document.dto.DocumentChunkResponse;
import com.jiacheng.securevault.document.dto.DocumentResponse;
import com.jiacheng.securevault.document.dto.DocumentReindexResponse;
import com.jiacheng.securevault.document.dto.EmbeddingStatusResponse;
import com.jiacheng.securevault.document.dto.SemanticSearchRequest;
import com.jiacheng.securevault.document.dto.SimilarChunkResponse;
import com.jiacheng.securevault.document.dto.DocumentTextResponse;
import com.jiacheng.securevault.document.dto.DocumentUpdateRequest;
import com.jiacheng.securevault.document.service.DocumentChunkService;
import com.jiacheng.securevault.document.service.DocumentEmbeddingService;
import com.jiacheng.securevault.document.service.DocumentParsingService;
import com.jiacheng.securevault.document.service.DocumentReindexService;
import com.jiacheng.securevault.document.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentParsingService documentParsingService;
    private final DocumentChunkService documentChunkService;
    private final DocumentEmbeddingService documentEmbeddingService;
    private final DocumentReindexService documentReindexService;

    public DocumentController(DocumentService documentService,
                              DocumentParsingService documentParsingService,
                              DocumentChunkService documentChunkService,
                              DocumentEmbeddingService documentEmbeddingService,
                              DocumentReindexService documentReindexService) {
        this.documentService = documentService;
        this.documentParsingService = documentParsingService;
        this.documentChunkService = documentChunkService;
        this.documentEmbeddingService = documentEmbeddingService;
        this.documentReindexService = documentReindexService;
    }

    @PostMapping
    public ApiResponse<DocumentResponse> create(@Valid @RequestBody DocumentCreateRequest request) {
        return ApiResponse.success(documentService.create(request));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentResponse> upload(@RequestParam("file") MultipartFile file,
                                                @RequestParam(value = "title", required = false) String title) {
        return ApiResponse.success(documentService.upload(file, title));
    }

    @GetMapping
    public ApiResponse<List<DocumentResponse>> list() {
        return ApiResponse.success(documentService.listCurrentUserDocuments());
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentResponse> get(@PathVariable Long id) {
        return ApiResponse.success(documentService.get(id));
    }

    @PostMapping("/{id}/parse")
    public ApiResponse<DocumentResponse> parse(@PathVariable Long id) {
        return ApiResponse.success(documentParsingService.parse(id));
    }

    @GetMapping("/{id}/text")
    public ApiResponse<DocumentTextResponse> getText(@PathVariable Long id) {
        return ApiResponse.success(documentParsingService.getText(id));
    }

    @PostMapping("/{id}/chunk")
    public ApiResponse<DocumentResponse> chunk(@PathVariable Long id) {
        return ApiResponse.success(documentChunkService.chunkDocument(id));
    }

    @GetMapping("/{id}/chunks")
    public ApiResponse<List<DocumentChunkResponse>> getChunks(@PathVariable Long id) {
        return ApiResponse.success(documentChunkService.listChunks(id));
    }

    @GetMapping("/{documentId}/chunks/{chunkIndex}")
    public ApiResponse<DocumentChunkDetailResponse> getChunkDetail(@PathVariable Long documentId,
                                                                   @PathVariable Integer chunkIndex) {
        return ApiResponse.success(documentChunkService.getChunkDetail(documentId, chunkIndex));
    }

    @PostMapping("/{id}/embed")
    public ApiResponse<DocumentResponse> embed(@PathVariable Long id) {
        return ApiResponse.success(documentEmbeddingService.embedDocument(id));
    }

    @PostMapping("/{documentId}/reindex")
    public ApiResponse<DocumentReindexResponse> reindex(@PathVariable Long documentId) {
        return ApiResponse.success(documentReindexService.reindexDocument(documentId));
    }

    @GetMapping("/{id}/embedding-status")
    public ApiResponse<EmbeddingStatusResponse> getEmbeddingStatus(@PathVariable Long id) {
        return ApiResponse.success(documentEmbeddingService.getEmbeddingStatus(id));
    }

    @PostMapping("/search-chunks")
    public ApiResponse<List<SimilarChunkResponse>> searchChunks(@Valid @RequestBody SemanticSearchRequest request) {
        return ApiResponse.success(documentEmbeddingService.searchSimilarChunks(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DocumentResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody DocumentUpdateRequest request) {
        return ApiResponse.success(documentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ApiResponse.success(null);
    }
}
