package com.graduationdesign.newsrecommendation.controller;

import com.graduationdesign.newsrecommendation.common.Result;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.security.CurrentUser;
import com.graduationdesign.newsrecommendation.service.UserBehaviorService;
import com.graduationdesign.newsrecommendation.vo.NewsActionStatusVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
public class UserBehaviorController {

    private final UserBehaviorService userBehaviorService;

    public UserBehaviorController(UserBehaviorService userBehaviorService) {
        this.userBehaviorService = userBehaviorService;
    }

    @PostMapping("/{id}/like")
    public Result<NewsActionStatusVO> like(@PathVariable Long id, @CurrentUser User currentUser) {
        return Result.success(userBehaviorService.likeNews(currentUser.getId(), id));
    }

    @DeleteMapping("/{id}/like")
    public Result<NewsActionStatusVO> unlike(@PathVariable Long id, @CurrentUser User currentUser) {
        return Result.success(userBehaviorService.unlikeNews(currentUser.getId(), id));
    }

    @PostMapping("/{id}/favorite")
    public Result<NewsActionStatusVO> favorite(@PathVariable Long id, @CurrentUser User currentUser) {
        return Result.success(userBehaviorService.favoriteNews(currentUser.getId(), id));
    }

    @DeleteMapping("/{id}/favorite")
    public Result<NewsActionStatusVO> unfavorite(@PathVariable Long id, @CurrentUser User currentUser) {
        return Result.success(userBehaviorService.unfavoriteNews(currentUser.getId(), id));
    }

    @PostMapping("/{id}/dislike")
    public Result<NewsActionStatusVO> dislike(@PathVariable Long id, @CurrentUser User currentUser) {
        return Result.success(userBehaviorService.dislikeNews(currentUser.getId(), id));
    }

    @PostMapping("/{id}/share")
    public Result<NewsActionStatusVO> share(@PathVariable Long id, @CurrentUser User currentUser) {
        return Result.success(userBehaviorService.shareNews(currentUser.getId(), id));
    }
}
