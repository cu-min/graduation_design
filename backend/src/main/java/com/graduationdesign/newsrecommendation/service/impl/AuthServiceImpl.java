package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduationdesign.newsrecommendation.dto.LoginRequest;
import com.graduationdesign.newsrecommendation.dto.RegisterRequest;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.exception.UnauthorizedException;
import com.graduationdesign.newsrecommendation.security.JwtTokenProvider;
import com.graduationdesign.newsrecommendation.service.AuthService;
import com.graduationdesign.newsrecommendation.service.UserService;
import com.graduationdesign.newsrecommendation.vo.CurrentUserVO;
import com.graduationdesign.newsrecommendation.vo.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserService userService, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public CurrentUserVO register(RegisterRequest request) {
        if (userService.lambdaQuery().eq(User::getUsername, request.getUsername()).exists()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userService.lambdaQuery().eq(User::getEmail, request.getEmail()).exists()) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail().trim());
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : request.getUsername().trim());
        user.setRole("USER");
        user.setStatus(1);

        userService.save(user);
        return toCurrentUserVO(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userService.getOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername().trim())
        );

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Username or password is incorrect");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new UnauthorizedException("Current account is disabled");
        }

        String token = jwtTokenProvider.generateToken(user);
        return new LoginResponse(token, toCurrentUserVO(user));
    }

    @Override
    public CurrentUserVO getCurrentUser(User user) {
        return toCurrentUserVO(user);
    }

    private CurrentUserVO toCurrentUserVO(User user) {
        CurrentUserVO currentUserVO = new CurrentUserVO();
        currentUserVO.setId(user.getId());
        currentUserVO.setUsername(user.getUsername());
        currentUserVO.setNickname(user.getNickname());
        currentUserVO.setEmail(user.getEmail());
        currentUserVO.setAvatar(user.getAvatar());
        currentUserVO.setRole(user.getRole());
        return currentUserVO;
    }
}
