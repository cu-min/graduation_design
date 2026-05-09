package com.graduationdesign.newsrecommendation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduationdesign.newsrecommendation.dto.CrawlConfigCreateRequest;
import com.graduationdesign.newsrecommendation.dto.CrawlConfigStatusRequest;
import com.graduationdesign.newsrecommendation.dto.CrawlConfigUpdateRequest;
import com.graduationdesign.newsrecommendation.entity.CrawlConfig;
import com.graduationdesign.newsrecommendation.vo.AdminCrawlConfigDetailVO;
import com.graduationdesign.newsrecommendation.vo.AdminCrawlConfigVO;
import com.graduationdesign.newsrecommendation.vo.CrawlRunResultVO;
import java.util.List;

public interface CrawlConfigService extends IService<CrawlConfig> {

    List<AdminCrawlConfigVO> listAdminCrawlConfigs();

    AdminCrawlConfigDetailVO getAdminCrawlConfigDetail(Long id);

    void createAdminCrawlConfig(CrawlConfigCreateRequest request);

    void updateAdminCrawlConfig(Long id, CrawlConfigUpdateRequest request);

    void deleteAdminCrawlConfig(Long id);

    void updateAdminCrawlConfigStatus(Long id, CrawlConfigStatusRequest request);

    CrawlRunResultVO runAdminCrawl(Long id);

    void runScheduledCrawls();
}
