package com.graduationdesign.newsrecommendation.service.impl;

import com.graduationdesign.newsrecommendation.service.CrawlConfigService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CrawlScheduler {

    private final CrawlConfigService crawlConfigService;

    public CrawlScheduler(CrawlConfigService crawlConfigService) {
        this.crawlConfigService = crawlConfigService;
    }

    @Scheduled(fixedDelay = 60000)
    public void scheduleDueCrawls() {
        crawlConfigService.runScheduledCrawls();
    }
}
