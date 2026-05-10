package com.graduationdesign.newsrecommendation.controller;

import com.graduationdesign.newsrecommendation.common.PageResult;
import com.graduationdesign.newsrecommendation.common.Result;
import com.graduationdesign.newsrecommendation.dto.PasswordUpdateRequest;
import com.graduationdesign.newsrecommendation.dto.ProfileUpdateRequest;
import com.graduationdesign.newsrecommendation.dto.UserInterestUpdateRequest;
import com.graduationdesign.newsrecommendation.entity.Tag;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.security.CurrentUser;
import com.graduationdesign.newsrecommendation.service.ProfileService;
import com.graduationdesign.newsrecommendation.service.UserInterestService;
import com.graduationdesign.newsrecommendation.service.UserService;
import com.graduationdesign.newsrecommendation.vo.CurrentUserVO;
import com.graduationdesign.newsrecommendation.vo.ProfileCommentVO;
import com.graduationdesign.newsrecommendation.vo.ProfileNewsItemVO;
import com.graduationdesign.newsrecommendation.vo.ProfileSummaryVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final UserInterestService userInterestService;
    private final UserService userService;

    public ProfileController(
        ProfileService profileService,
        UserInterestService userInterestService,
        UserService userService
    ) {
        this.profileService = profileService;
        this.userInterestService = userInterestService;
        this.userService = userService;
    }

    @GetMapping("/summary")
    public Result<ProfileSummaryVO> summary(@CurrentUser User currentUser) {
        return Result.success(profileService.getProfileSummary(currentUser));
    }

    @GetMapping("/favorites")
    public Result<PageResult<ProfileNewsItemVO>> favorites(
        @CurrentUser User currentUser,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size
    ) {
        return Result.success(profileService.pageFavorites(currentUser.getId(), page, size));
    }

    @GetMapping("/likes")
    public Result<PageResult<ProfileNewsItemVO>> likes(
        @CurrentUser User currentUser,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size
    ) {
        return Result.success(profileService.pageLikes(currentUser.getId(), page, size));
    }

    @GetMapping("/history")
    public Result<PageResult<ProfileNewsItemVO>> history(
        @CurrentUser User currentUser,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size
    ) {
        return Result.success(profileService.pageHistory(currentUser.getId(), page, size));
    }

    @GetMapping("/comments")
    public Result<PageResult<ProfileCommentVO>> comments(
        @CurrentUser User currentUser,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size
    ) {
        return Result.success(profileService.pageComments(currentUser.getId(), page, size));
    }

    @GetMapping("/interests")
    public Result<List<Tag>> interests(@CurrentUser User currentUser) {
        return Result.success(userInterestService.listCurrentUserInterests(currentUser.getId()));
    }

    @PutMapping("/basic")
    public Result<CurrentUserVO> updateBasic(
        @CurrentUser User currentUser,
        @Valid @RequestBody ProfileUpdateRequest request
    ) {
        return Result.success(userService.updateCurrentUserProfile(currentUser, request));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(
        @CurrentUser User currentUser,
        @Valid @RequestBody PasswordUpdateRequest request
    ) {
        userService.updateCurrentUserPassword(currentUser, request);
        return Result.success();
    }

    @PutMapping("/interests")
    public Result<Void> updateInterests(
        @CurrentUser User currentUser,
        @Valid @RequestBody UserInterestUpdateRequest request
    ) {
        userInterestService.updateCurrentUserInterests(currentUser, request.getTagIds());
        return Result.success();
    }
}
