package com.graduationdesign.newsrecommendation.service;

import com.graduationdesign.newsrecommendation.common.PageResult;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.vo.RecommendNewsVO;

public interface RecommendService {

    PageResult<RecommendNewsVO> pageRecommendNews(User currentUser, long page, long size);
}
