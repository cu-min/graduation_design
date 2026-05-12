package com.graduationdesign.newsrecommendation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduationdesign.newsrecommendation.dto.AdminTagRequest;
import com.graduationdesign.newsrecommendation.entity.Tag;
import java.util.List;

public interface TagService extends IService<Tag> {

    List<Tag> listPublicTags(Long categoryId);

    List<Tag> listAdminTags(Long categoryId);

    void createAdminTag(AdminTagRequest request);

    void updateAdminTag(Long id, AdminTagRequest request);

    void deleteAdminTag(Long id);

    void updateAdminTagStatus(Long id, Integer status);
}
