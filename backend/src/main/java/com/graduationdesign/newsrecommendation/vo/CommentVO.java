package com.graduationdesign.newsrecommendation.vo;

import java.time.LocalDateTime;
import java.util.List;

public class CommentVO {

    private Long id;
    private Long newsId;
    private Long userId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
    private Boolean canDelete;
    private List<CommentReplyVO> replies;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNewsId() {
        return newsId;
    }

    public void setNewsId(Long newsId) {
        this.newsId = newsId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getCanDelete() {
        return canDelete;
    }

    public void setCanDelete(Boolean canDelete) {
        this.canDelete = canDelete;
    }

    public List<CommentReplyVO> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentReplyVO> replies) {
        this.replies = replies;
    }
}
