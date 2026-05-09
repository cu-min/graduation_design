package com.graduationdesign.newsrecommendation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduationdesign.newsrecommendation.entity.UserInterest;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.entity.Tag;
import java.util.List;

public interface UserInterestService extends IService<UserInterest> {

    List<Tag> listCurrentUserInterests(Long userId);

    void updateCurrentUserInterests(User currentUser, List<Long> tagIds);
}
