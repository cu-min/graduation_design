package com.graduationdesign.newsrecommendation.controller;

import com.graduationdesign.newsrecommendation.common.Result;
import com.graduationdesign.newsrecommendation.dto.LoginRequest;
import com.graduationdesign.newsrecommendation.dto.RegisterRequest;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.security.CurrentUser;
import com.graduationdesign.newsrecommendation.service.AuthService;
import com.graduationdesign.newsrecommendation.vo.CurrentUserVO;
import com.graduationdesign.newsrecommendation.vo.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<CurrentUserVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<CurrentUserVO> me(@CurrentUser User currentUser) {
        return Result.success(authService.getCurrentUser(currentUser));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
