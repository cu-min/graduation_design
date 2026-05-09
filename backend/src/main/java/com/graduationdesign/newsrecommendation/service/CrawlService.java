package com.graduationdesign.newsrecommendation.service;

import com.graduationdesign.newsrecommendation.entity.CrawlConfig;
import com.graduationdesign.newsrecommendation.vo.CrawlRunResultVO;

public interface CrawlService {

    CrawlRunResultVO runRssCrawl(CrawlConfig crawlConfig);
}
