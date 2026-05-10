package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduationdesign.newsrecommendation.common.PageResult;
import com.graduationdesign.newsrecommendation.dto.PasswordUpdateRequest;
import com.graduationdesign.newsrecommendation.dto.ProfileUpdateRequest;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.exception.NotFoundException;
import com.graduationdesign.newsrecommendation.exception.UnauthorizedException;
import com.graduationdesign.newsrecommendation.mapper.UserMapper;
import com.graduationdesign.newsrecommendation.service.UserService;
import com.graduationdesign.newsrecommendation.vo.AdminUserVO;
import com.graduationdesign.newsrecommendation.vo.CurrentUserVO;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<AdminUserVO> pageAdminUsers(long page, long size, String keyword, String role, Integer status) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.max(size, 1);

        Page<User> mpPage = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>()
            .eq(StringUtils.hasText(role), User::getRole, role)
            .eq(status != null, User::getStatus, status)
            .and(StringUtils.hasText(keyword), wrapper -> wrapper
                .like(User::getUsername, keyword)
                .or()
                .like(User::getNickname, keyword)
                .or()
                .like(User::getEmail, keyword)
                .or()
                .like(User::getPhone, keyword)
            )
            .orderByDesc(User::getCreatedAt)
            .orderByDesc(User::getId);

        Page<User> pageResult = page(mpPage, queryWrapper);
        List<AdminUserVO> records = pageResult.getRecords().stream().map(this::toAdminUserVO).toList();
        return new PageResult<>(records, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }

    @Override
    @Transactional
    public void updateAdminUserStatus(Long id, Integer status, Long operatorUserId) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("Status must be 0 or 1");
        }
        if (status == 0 && id.equals(operatorUserId)) {
            throw new IllegalArgumentException("You cannot disable your own account");
        }
        User user = getByIdOrThrow(id);
        user.setStatus(status);
        updateById(user);
    }

    @Override
    @Transactional
    public CurrentUserVO updateCurrentUserProfile(User currentUser, ProfileUpdateRequest request) {
        User user = getByIdOrThrow(currentUser.getId());
        validateUniqueEmail(request.getEmail(), user.getId());

        user.setNickname(request.getNickname().trim());
        user.setEmail(request.getEmail().trim());
        user.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);
        user.setAvatar(StringUtils.hasText(request.getAvatar()) ? request.getAvatar().trim() : null);
        updateById(user);

        return toCurrentUserVO(user);
    }

    @Override
    @Transactional
    public void updateCurrentUserPassword(User currentUser, PasswordUpdateRequest request) {
        User user = getByIdOrThrow(currentUser.getId());
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as the current password");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        updateById(user);
    }

    private void validateUniqueEmail(String email, Long excludeId) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>()
            .eq(User::getEmail, email.trim());
        if (excludeId != null) {
            queryWrapper.ne(User::getId, excludeId);
        }
        if (count(queryWrapper) > 0) {
            throw new IllegalArgumentException("Email already exists");
        }
    }

    private User getByIdOrThrow(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new NotFoundException("User does not exist");
        }
        return user;
    }

    private AdminUserVO toAdminUserVO(User user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }

    private CurrentUserVO toCurrentUserVO(User user) {
        CurrentUserVO currentUserVO = new CurrentUserVO();
        currentUserVO.setId(user.getId());
        currentUserVO.setUsername(user.getUsername());
        currentUserVO.setNickname(user.getNickname());
        currentUserVO.setEmail(user.getEmail());
        currentUserVO.setPhone(user.getPhone());
        currentUserVO.setAvatar(user.getAvatar());
        currentUserVO.setRole(user.getRole());
        return currentUserVO;
    }
}
