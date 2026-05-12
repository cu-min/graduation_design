package com.graduationdesign.newsrecommendation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduationdesign.newsrecommendation.common.Result;
import com.graduationdesign.newsrecommendation.entity.Tag;
import com.graduationdesign.newsrecommendation.service.TagService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public Result<List<Tag>> list(@RequestParam(required = false) Long categoryId) {
        return Result.success(tagService.listPublicTags(categoryId));
    }
}
