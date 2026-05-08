package com.jiacheng.securevault.document.dto;

import jakarta.validation.constraints.NotBlank;

public class DocumentCreateRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
