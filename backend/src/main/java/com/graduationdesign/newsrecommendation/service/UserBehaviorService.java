package com.graduationdesign.newsrecommendation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduationdesign.newsrecommendation.entity.UserBehavior;
import com.graduationdesign.newsrecommendation.vo.NewsActionStatusVO;

public interface UserBehaviorService extends IService<UserBehavior> {

    void recordView(Long userId, Long newsId);

    NewsActionStatusVO likeNews(Long userId, Long newsId);

    NewsActionStatusVO unlikeNews(Long userId, Long newsId);

    NewsActionStatusVO favoriteNews(Long userId, Long newsId);

    NewsActionStatusVO unfavoriteNews(Long userId, Long newsId);

    NewsActionStatusVO dislikeNews(Long userId, Long newsId);

    NewsActionStatusVO shareNews(Long userId, Long newsId);

    NewsActionStatusVO getActionStatus(Long userId, Long newsId);
}
