package com.graduationdesign.newsrecommendation.controller;

import com.graduationdesign.newsrecommendation.common.Result;
import com.graduationdesign.newsrecommendation.dto.CrawlConfigCreateRequest;
import com.graduationdesign.newsrecommendation.dto.CrawlConfigStatusRequest;
import com.graduationdesign.newsrecommendation.dto.CrawlConfigUpdateRequest;
import com.graduationdesign.newsrecommendation.service.CrawlConfigService;
import com.graduationdesign.newsrecommendation.vo.AdminCrawlConfigDetailVO;
import com.graduationdesign.newsrecommendation.vo.AdminCrawlConfigVO;
import com.graduationdesign.newsrecommendation.vo.CrawlRunResultVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/crawl-configs")
public class AdminCrawlConfigController {

    private final CrawlConfigService crawlConfigService;

    public AdminCrawlConfigController(CrawlConfigService crawlConfigService) {
        this.crawlConfigService = crawlConfigService;
    }

    @GetMapping
    public Result<List<AdminCrawlConfigVO>> list() {
        return Result.success(crawlConfigService.listAdminCrawlConfigs());
    }

    @GetMapping("/{id}")
    public Result<AdminCrawlConfigDetailVO> detail(@PathVariable Long id) {
        return Result.success(crawlConfigService.getAdminCrawlConfigDetail(id));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody CrawlConfigCreateRequest request) {
        crawlConfigService.createAdminCrawlConfig(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CrawlConfigUpdateRequest request) {
        crawlConfigService.updateAdminCrawlConfig(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        crawlConfigService.deleteAdminCrawlConfig(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody CrawlConfigStatusRequest request) {
        crawlConfigService.updateAdminCrawlConfigStatus(id, request);
        return Result.success();
    }

    @PostMapping("/{id}/run")
    public Result<CrawlRunResultVO> run(@PathVariable Long id) {
        return Result.success(crawlConfigService.runAdminCrawl(id));
    }
}
