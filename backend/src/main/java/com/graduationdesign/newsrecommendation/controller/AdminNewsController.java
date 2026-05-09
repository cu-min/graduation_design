package com.graduationdesign.newsrecommendation.controller;

import com.graduationdesign.newsrecommendation.common.PageResult;
import com.graduationdesign.newsrecommendation.common.Result;
import com.graduationdesign.newsrecommendation.dto.NewsCreateRequest;
import com.graduationdesign.newsrecommendation.dto.NewsStatusRequest;
import com.graduationdesign.newsrecommendation.dto.NewsUpdateRequest;
import com.graduationdesign.newsrecommendation.service.NewsService;
import com.graduationdesign.newsrecommendation.vo.AdminNewsDetailVO;
import com.graduationdesign.newsrecommendation.vo.AdminNewsListVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/news")
public class AdminNewsController {

    private final NewsService newsService;

    public AdminNewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public Result<PageResult<AdminNewsListVO>> list(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Integer status
    ) {
        return Result.success(newsService.pageAdminNews(page, size, keyword, categoryId, status));
    }

    @GetMapping("/{id}")
    public Result<AdminNewsDetailVO> detail(@PathVariable Long id) {
        return Result.success(newsService.getAdminNewsDetail(id));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody NewsCreateRequest request) {
        newsService.createAdminNews(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody NewsUpdateRequest request) {
        newsService.updateAdminNews(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        newsService.deleteAdminNews(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody NewsStatusRequest request) {
        newsService.updateAdminNewsStatus(id, request);
        return Result.success();
    }
}
