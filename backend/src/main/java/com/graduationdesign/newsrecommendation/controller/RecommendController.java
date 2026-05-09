package com.graduationdesign.newsrecommendation.controller;

import com.graduationdesign.newsrecommendation.common.PageResult;
import com.graduationdesign.newsrecommendation.common.Result;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.security.CurrentUser;
import com.graduationdesign.newsrecommendation.service.RecommendService;
import com.graduationdesign.newsrecommendation.vo.RecommendNewsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping("/news")
    public Result<PageResult<RecommendNewsVO>> pageRecommendNews(
        @CurrentUser User currentUser,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size
    ) {
        return Result.success(recommendService.pageRecommendNews(currentUser, page, size));
    }
}
