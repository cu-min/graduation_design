package com.graduationdesign.newsrecommendation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduationdesign.newsrecommendation.common.PageResult;
import com.graduationdesign.newsrecommendation.dto.PasswordUpdateRequest;
import com.graduationdesign.newsrecommendation.dto.ProfileUpdateRequest;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.vo.AdminUserVO;
import com.graduationdesign.newsrecommendation.vo.CurrentUserVO;

public interface UserService extends IService<User> {

    PageResult<AdminUserVO> pageAdminUsers(long page, long size, String keyword, String role, Integer status);

    void updateAdminUserStatus(Long id, Integer status, Long operatorUserId);

    CurrentUserVO updateCurrentUserProfile(User currentUser, ProfileUpdateRequest request);

    void updateCurrentUserPassword(User currentUser, PasswordUpdateRequest request);
}
