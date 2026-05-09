package com.graduationdesign.newsrecommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public class NewsCreateRequest {

    @NotBlank(message = "Title cannot be blank")
    private String title;

    @NotBlank(message = "Summary cannot be blank")
    private String summary;

    @NotBlank(message = "Content cannot be blank")
    private String content;

    @NotBlank(message = "Source name cannot be blank")
    private String sourceName;

    @NotBlank(message = "Source URL cannot be blank")
    private String sourceUrl;

    @NotBlank(message = "Cover image cannot be blank")
    private String coverImage;

    @NotNull(message = "Category cannot be empty")
    private Long categoryId;

    @NotEmpty(message = "Tags cannot be empty")
    private List<Long> tagIds;

    @NotNull(message = "Publish time cannot be empty")
    private LocalDateTime publishTime;

    @NotNull(message = "Heat score cannot be empty")
    private Double heatScore;

    @NotNull(message = "Status cannot be empty")
    private Integer status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

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

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public Double getHeatScore() {
        return heatScore;
    }

    public void setHeatScore(Double heatScore) {
        this.heatScore = heatScore;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
