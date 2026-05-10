package com.graduationdesign.newsrecommendation.controller;

import com.graduationdesign.newsrecommendation.common.PageResult;
import com.graduationdesign.newsrecommendation.common.Result;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.security.CurrentUser;
import com.graduationdesign.newsrecommendation.service.NewsService;
import com.graduationdesign.newsrecommendation.vo.HotNewsVO;
import com.graduationdesign.newsrecommendation.vo.NewsDetailVO;
import com.graduationdesign.newsrecommendation.vo.NewsListVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public Result<PageResult<NewsListVO>> list(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId
    ) {
        return Result.success(newsService.pagePublicNews(page, size, keyword, categoryId));
    }

    @GetMapping("/hot")
    public Result<List<HotNewsVO>> hot(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(newsService.listHotNews(limit));
    }

    @GetMapping("/{id}")
    public Result<NewsDetailVO> detail(@PathVariable Long id, @CurrentUser User currentUser) {
        return Result.success(newsService.getPublicNewsDetail(id, currentUser));
    }

    @GetMapping("/{id}/related")
    public Result<List<NewsListVO>> related(
        @PathVariable Long id,
        @RequestParam(defaultValue = "4") int limit
    ) {
        return Result.success(newsService.listRelatedNews(id, limit));
    }
}
