package com.graduationdesign.newsrecommendation.service;

import com.graduationdesign.newsrecommendation.dto.LoginRequest;
import com.graduationdesign.newsrecommendation.dto.RegisterRequest;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.vo.CurrentUserVO;
import com.graduationdesign.newsrecommendation.vo.LoginResponse;

public interface AuthService {

    CurrentUserVO register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    CurrentUserVO getCurrentUser(User user);
}
