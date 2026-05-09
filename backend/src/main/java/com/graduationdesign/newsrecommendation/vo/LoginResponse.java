package com.graduationdesign.newsrecommendation.vo;

public class LoginResponse {

    private String token;
    private CurrentUserVO user;

    public LoginResponse() {
    }

    public LoginResponse(String token, CurrentUserVO user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public CurrentUserVO getUser() {
        return user;
    }

    public void setUser(CurrentUserVO user) {
        this.user = user;
    }
}
