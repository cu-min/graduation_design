package com.graduationdesign.newsrecommendation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CrawlConfigCreateRequest {

    @NotBlank(message = "Source name cannot be blank")
    private String sourceName;

    @NotBlank(message = "Source URL cannot be blank")
    private String sourceUrl;

    @NotBlank(message = "Source type cannot be blank")
    private String sourceType;

    @NotNull(message = "Category cannot be empty")
    private Long categoryId;

    @NotNull(message = "Enabled status cannot be empty")
    @Min(value = 0, message = "Enabled status must be 0 or 1")
    @Max(value = 1, message = "Enabled status must be 0 or 1")
    private Integer enabled;

    @NotNull(message = "Crawl interval cannot be empty")
    @Min(value = 1, message = "Crawl interval must be greater than 0")
    private Integer crawlInterval;

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public Integer getCrawlInterval() {
        return crawlInterval;
    }

    public void setCrawlInterval(Integer crawlInterval) {
        this.crawlInterval = crawlInterval;
    }
}
