package com.graduationdesign.newsrecommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminTagRequest {

    @NotNull(message = "Category cannot be empty")
    private Long categoryId;

    @NotBlank(message = "Tag name cannot be blank")
    @Size(max = 50, message = "Tag name length must not exceed 50")
    private String name;

    @NotBlank(message = "Tag code cannot be blank")
    @Size(max = 50, message = "Tag code length must not exceed 50")
    private String code;

    @NotNull(message = "Sort order cannot be empty")
    private Integer sortOrder;

    @NotNull(message = "Status cannot be empty")
    private Integer status;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
