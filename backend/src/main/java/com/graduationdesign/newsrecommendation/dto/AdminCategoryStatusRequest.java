package com.graduationdesign.newsrecommendation.dto;

import jakarta.validation.constraints.NotNull;

public class AdminCategoryStatusRequest {

    @NotNull(message = "Status cannot be empty")
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
