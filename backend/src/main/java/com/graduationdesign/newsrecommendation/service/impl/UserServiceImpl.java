package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.mapper.UserMapper;
import com.graduationdesign.newsrecommendation.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
