package com.graduationdesign.newsrecommendation.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class UserInterestUpdateRequest {

    @NotNull(message = "tagIds cannot be null")
    private List<Long> tagIds;

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }
}
