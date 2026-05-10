package com.graduationdesign.newsrecommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminCategoryRequest {

    @NotBlank(message = "Category name cannot be blank")
    @Size(max = 50, message = "Category name length must not exceed 50")
    private String name;

    @NotBlank(message = "Category code cannot be blank")
    @Size(max = 50, message = "Category code length must not exceed 50")
    private String code;

    @Size(max = 255, message = "Category description length must not exceed 255")
    private String description;

    @NotNull(message = "Sort order cannot be empty")
    private Integer sortOrder;

    @NotNull(message = "Status cannot be empty")
    private Integer status;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
