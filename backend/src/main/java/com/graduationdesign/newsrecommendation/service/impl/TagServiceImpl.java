package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduationdesign.newsrecommendation.dto.AdminTagRequest;
import com.graduationdesign.newsrecommendation.entity.Category;
import com.graduationdesign.newsrecommendation.entity.NewsTag;
import com.graduationdesign.newsrecommendation.entity.Tag;
import com.graduationdesign.newsrecommendation.entity.UserInterest;
import com.graduationdesign.newsrecommendation.exception.NotFoundException;
import com.graduationdesign.newsrecommendation.mapper.CategoryMapper;
import com.graduationdesign.newsrecommendation.mapper.NewsTagMapper;
import com.graduationdesign.newsrecommendation.mapper.TagMapper;
import com.graduationdesign.newsrecommendation.mapper.UserInterestMapper;
import com.graduationdesign.newsrecommendation.service.TagService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    private final CategoryMapper categoryMapper;
    private final NewsTagMapper newsTagMapper;
    private final UserInterestMapper userInterestMapper;

    public TagServiceImpl(
        CategoryMapper categoryMapper,
        NewsTagMapper newsTagMapper,
        UserInterestMapper userInterestMapper
    ) {
        this.categoryMapper = categoryMapper;
        this.newsTagMapper = newsTagMapper;
        this.userInterestMapper = userInterestMapper;
    }

    @Override
    public List<Tag> listAdminTags(Long categoryId) {
        return list(new LambdaQueryWrapper<Tag>()
            .eq(categoryId != null, Tag::getCategoryId, categoryId)
            .orderByAsc(Tag::getSortOrder)
            .orderByAsc(Tag::getId));
    }

    @Override
    @Transactional
    public void createAdminTag(AdminTagRequest request) {
        validateStatus(request.getStatus());
        validateCategoryExists(request.getCategoryId());
        validateUniqueCode(request.getCode(), null);

        Tag tag = new Tag();
        fillTag(tag, request);
        save(tag);
    }

    @Override
    @Transactional
    public void updateAdminTag(Long id, AdminTagRequest request) {
        validateStatus(request.getStatus());
        validateCategoryExists(request.getCategoryId());
        Tag tag = getByIdOrThrow(id);
        validateUniqueCode(request.getCode(), id);

        fillTag(tag, request);
        updateById(tag);
    }

    @Override
    @Transactional
    public void deleteAdminTag(Long id) {
        getByIdOrThrow(id);
        if (newsTagMapper.selectCount(new LambdaQueryWrapper<NewsTag>().eq(NewsTag::getTagId, id)) > 0) {
            throw new IllegalArgumentException("Current tag is already bound to news and cannot be deleted");
        }
        if (userInterestMapper.selectCount(new LambdaQueryWrapper<UserInterest>().eq(UserInterest::getTagId, id)) > 0) {
            throw new IllegalArgumentException("Current tag is already used by user interests and cannot be deleted");
        }
        removeById(id);
    }

    @Override
    @Transactional
    public void updateAdminTagStatus(Long id, Integer status) {
        validateStatus(status);
        Tag tag = getByIdOrThrow(id);
        tag.setStatus(status);
        updateById(tag);
    }

    private void validateCategoryExists(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Category does not exist");
        }
    }

    private Tag getByIdOrThrow(Long id) {
        Tag tag = getById(id);
        if (tag == null) {
            throw new NotFoundException("Tag does not exist");
        }
        return tag;
    }

    private void fillTag(Tag tag, AdminTagRequest request) {
        tag.setCategoryId(request.getCategoryId());
        tag.setName(request.getName().trim());
        tag.setCode(request.getCode().trim());
        tag.setSortOrder(request.getSortOrder());
        tag.setStatus(request.getStatus());
    }

    private void validateUniqueCode(String code, Long excludeId) {
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<Tag>()
            .eq(Tag::getCode, code.trim());
        if (excludeId != null) {
            queryWrapper.ne(Tag::getId, excludeId);
        }
        if (count(queryWrapper) > 0) {
            throw new IllegalArgumentException("Tag code already exists");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("Status must be 0 or 1");
        }
    }
}
