package com.graduationdesign.newsrecommendation.vo;

public class CrawlRunResultVO {

    private Long crawlConfigId;
    private String sourceName;
    private Integer insertedCount;
    private Integer duplicateCount;
    private String lastStatus;
    private String message;

    public Long getCrawlConfigId() {
        return crawlConfigId;
    }

    public void setCrawlConfigId(Long crawlConfigId) {
        this.crawlConfigId = crawlConfigId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public Integer getInsertedCount() {
        return insertedCount;
    }

    public void setInsertedCount(Integer insertedCount) {
        this.insertedCount = insertedCount;
    }

    public Integer getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(Integer duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
