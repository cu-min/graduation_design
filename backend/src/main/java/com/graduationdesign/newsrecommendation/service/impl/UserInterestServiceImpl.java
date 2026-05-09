package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduationdesign.newsrecommendation.entity.Tag;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.entity.UserInterest;
import com.graduationdesign.newsrecommendation.mapper.TagMapper;
import com.graduationdesign.newsrecommendation.mapper.UserInterestMapper;
import com.graduationdesign.newsrecommendation.service.UserInterestService;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserInterestServiceImpl extends ServiceImpl<UserInterestMapper, UserInterest> implements UserInterestService {

    private final TagMapper tagMapper;

    public UserInterestServiceImpl(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    @Override
    public List<Tag> listCurrentUserInterests(Long userId) {
        List<UserInterest> interests = list(new LambdaQueryWrapper<UserInterest>()
            .eq(UserInterest::getUserId, userId)
            .orderByAsc(UserInterest::getId));
        if (interests.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = interests.stream().map(UserInterest::getTagId).toList();
        return tagMapper.selectBatchIds(tagIds).stream()
            .filter(tag -> tag.getStatus() != null && tag.getStatus() == 1)
            .toList();
    }

    @Override
    @Transactional
    public void updateCurrentUserInterests(User currentUser, List<Long> tagIds) {
        List<Long> safeTagIds = tagIds == null ? Collections.emptyList() : tagIds;
        Set<Long> distinctTagIds = new LinkedHashSet<>(safeTagIds);
        if (!distinctTagIds.isEmpty()) {
            List<Tag> tags = tagMapper.selectBatchIds(distinctTagIds);
            if (tags.size() != distinctTagIds.size() || tags.stream().anyMatch(tag -> tag.getStatus() == null || tag.getStatus() != 1)) {
                throw new IllegalArgumentException("Some tags do not exist or are unavailable");
            }
        }

        remove(new LambdaQueryWrapper<UserInterest>().eq(UserInterest::getUserId, currentUser.getId()));

        for (Long tagId : distinctTagIds) {
            UserInterest interest = new UserInterest();
            interest.setUserId(currentUser.getId());
            interest.setTagId(tagId);
            interest.setWeight(1.0);
            save(interest);
        }
    }
}
