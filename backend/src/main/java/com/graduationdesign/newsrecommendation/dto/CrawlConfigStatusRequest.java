package com.graduationdesign.newsrecommendation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CrawlConfigStatusRequest {

    @NotNull(message = "Enabled status cannot be empty")
    @Min(value = 0, message = "Enabled status must be 0 or 1")
    @Max(value = 1, message = "Enabled status must be 0 or 1")
    private Integer enabled;

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}
