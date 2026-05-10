package com.jiacheng.securevault.document.controller;

import com.jiacheng.securevault.common.ApiResponse;
import com.jiacheng.securevault.document.dto.DocumentCreateRequest;
import com.jiacheng.securevault.document.dto.DocumentResponse;
import com.jiacheng.securevault.document.dto.DocumentUpdateRequest;
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

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
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
