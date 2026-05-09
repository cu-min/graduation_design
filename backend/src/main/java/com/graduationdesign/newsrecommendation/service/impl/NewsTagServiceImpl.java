package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduationdesign.newsrecommendation.entity.NewsTag;
import com.graduationdesign.newsrecommendation.mapper.NewsTagMapper;
import com.graduationdesign.newsrecommendation.service.NewsTagService;
import org.springframework.stereotype.Service;

@Service
public class NewsTagServiceImpl extends ServiceImpl<NewsTagMapper, NewsTag> implements NewsTagService {
}
