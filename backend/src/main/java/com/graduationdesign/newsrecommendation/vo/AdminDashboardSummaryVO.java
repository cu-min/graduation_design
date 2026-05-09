package com.graduationdesign.newsrecommendation.vo;

import java.time.LocalDateTime;
import java.util.List;

public class AdminDashboardSummaryVO {

    private Long newsTotal;
    private Long onlineNewsTotal;
    private Long offlineNewsTotal;
    private Long userTotal;
    private Long commentTotal;
    private Long crawlConfigTotal;
    private Long enabledCrawlConfigTotal;
    private Long todayCrawledNewsTotal;
    private Long todayCommentTotal;
    private Long viewBehaviorTotal;
    private Long likeBehaviorTotal;
    private Long favoriteBehaviorTotal;
    private Long dislikeBehaviorTotal;
    private Long shareBehaviorTotal;
    private LocalDateTime latestCrawlTime;
    private String latestCrawlStatus;
    private List<AdminHotNewsVO> hotNews;
    private List<AdminCategoryStatVO> categoryStats;

    public Long getNewsTotal() {
        return newsTotal;
    }

    public void setNewsTotal(Long newsTotal) {
        this.newsTotal = newsTotal;
    }

    public Long getOnlineNewsTotal() {
        return onlineNewsTotal;
    }

    public void setOnlineNewsTotal(Long onlineNewsTotal) {
        this.onlineNewsTotal = onlineNewsTotal;
    }

    public Long getOfflineNewsTotal() {
        return offlineNewsTotal;
    }

    public void setOfflineNewsTotal(Long offlineNewsTotal) {
        this.offlineNewsTotal = offlineNewsTotal;
    }

    public Long getUserTotal() {
        return userTotal;
    }

    public void setUserTotal(Long userTotal) {
        this.userTotal = userTotal;
    }

    public Long getCommentTotal() {
        return commentTotal;
    }

    public void setCommentTotal(Long commentTotal) {
        this.commentTotal = commentTotal;
    }

    public Long getCrawlConfigTotal() {
        return crawlConfigTotal;
    }

    public void setCrawlConfigTotal(Long crawlConfigTotal) {
        this.crawlConfigTotal = crawlConfigTotal;
    }

    public Long getEnabledCrawlConfigTotal() {
        return enabledCrawlConfigTotal;
    }

    public void setEnabledCrawlConfigTotal(Long enabledCrawlConfigTotal) {
        this.enabledCrawlConfigTotal = enabledCrawlConfigTotal;
    }

    public Long getTodayCrawledNewsTotal() {
        return todayCrawledNewsTotal;
    }

    public void setTodayCrawledNewsTotal(Long todayCrawledNewsTotal) {
        this.todayCrawledNewsTotal = todayCrawledNewsTotal;
    }

    public Long getTodayCommentTotal() {
        return todayCommentTotal;
    }

    public void setTodayCommentTotal(Long todayCommentTotal) {
        this.todayCommentTotal = todayCommentTotal;
    }

    public Long getViewBehaviorTotal() {
        return viewBehaviorTotal;
    }

    public void setViewBehaviorTotal(Long viewBehaviorTotal) {
        this.viewBehaviorTotal = viewBehaviorTotal;
    }

    public Long getLikeBehaviorTotal() {
        return likeBehaviorTotal;
    }

    public void setLikeBehaviorTotal(Long likeBehaviorTotal) {
        this.likeBehaviorTotal = likeBehaviorTotal;
    }

    public Long getFavoriteBehaviorTotal() {
        return favoriteBehaviorTotal;
    }

    public void setFavoriteBehaviorTotal(Long favoriteBehaviorTotal) {
        this.favoriteBehaviorTotal = favoriteBehaviorTotal;
    }

    public Long getDislikeBehaviorTotal() {
        return dislikeBehaviorTotal;
    }

    public void setDislikeBehaviorTotal(Long dislikeBehaviorTotal) {
        this.dislikeBehaviorTotal = dislikeBehaviorTotal;
    }

    public Long getShareBehaviorTotal() {
        return shareBehaviorTotal;
    }

    public void setShareBehaviorTotal(Long shareBehaviorTotal) {
        this.shareBehaviorTotal = shareBehaviorTotal;
    }

    public LocalDateTime getLatestCrawlTime() {
        return latestCrawlTime;
    }

    public void setLatestCrawlTime(LocalDateTime latestCrawlTime) {
        this.latestCrawlTime = latestCrawlTime;
    }

    public String getLatestCrawlStatus() {
        return latestCrawlStatus;
    }

    public void setLatestCrawlStatus(String latestCrawlStatus) {
        this.latestCrawlStatus = latestCrawlStatus;
    }

    public List<AdminHotNewsVO> getHotNews() {
        return hotNews;
    }

    public void setHotNews(List<AdminHotNewsVO> hotNews) {
        this.hotNews = hotNews;
    }

    public List<AdminCategoryStatVO> getCategoryStats() {
        return categoryStats;
    }

    public void setCategoryStats(List<AdminCategoryStatVO> categoryStats) {
        this.categoryStats = categoryStats;
    }
}
