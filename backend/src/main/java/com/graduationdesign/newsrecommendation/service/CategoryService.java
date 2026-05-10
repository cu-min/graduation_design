package com.graduationdesign.newsrecommendation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduationdesign.newsrecommendation.dto.AdminCategoryRequest;
import com.graduationdesign.newsrecommendation.entity.Category;
import java.util.List;

public interface CategoryService extends IService<Category> {

    List<Category> listAdminCategories();

    void createAdminCategory(AdminCategoryRequest request);

    void updateAdminCategory(Long id, AdminCategoryRequest request);

    void deleteAdminCategory(Long id);

    void updateAdminCategoryStatus(Long id, Integer status);
}
