package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduationdesign.newsrecommendation.entity.Category;
import com.graduationdesign.newsrecommendation.mapper.CategoryMapper;
import com.graduationdesign.newsrecommendation.service.CategoryService;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
}
