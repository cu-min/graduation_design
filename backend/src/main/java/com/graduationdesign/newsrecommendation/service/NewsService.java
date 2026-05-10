package com.graduationdesign.newsrecommendation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduationdesign.newsrecommendation.common.PageResult;
import com.graduationdesign.newsrecommendation.dto.NewsCreateRequest;
import com.graduationdesign.newsrecommendation.dto.NewsStatusRequest;
import com.graduationdesign.newsrecommendation.dto.NewsUpdateRequest;
import com.graduationdesign.newsrecommendation.entity.News;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.vo.AdminNewsDetailVO;
import com.graduationdesign.newsrecommendation.vo.AdminNewsListVO;
import com.graduationdesign.newsrecommendation.vo.HotNewsVO;
import com.graduationdesign.newsrecommendation.vo.NewsDetailVO;
import com.graduationdesign.newsrecommendation.vo.NewsListVO;
import java.util.List;

public interface NewsService extends IService<News> {

    PageResult<AdminNewsListVO> pageAdminNews(long page, long size, String keyword, Long categoryId, Integer status);

    AdminNewsDetailVO getAdminNewsDetail(Long id);

    void createAdminNews(NewsCreateRequest request);

    void updateAdminNews(Long id, NewsUpdateRequest request);

    void deleteAdminNews(Long id);

    void updateAdminNewsStatus(Long id, NewsStatusRequest request);

    PageResult<NewsListVO> pagePublicNews(long page, long size, String keyword, Long categoryId);

    NewsDetailVO getPublicNewsDetail(Long id, User currentUser);

    List<NewsListVO> listRelatedNews(Long id, int limit);

    List<HotNewsVO> listHotNews(int limit);
}
