package com.graduationdesign.newsrecommendation.controller;

import com.graduationdesign.newsrecommendation.common.PageResult;
import com.graduationdesign.newsrecommendation.common.Result;
import com.graduationdesign.newsrecommendation.config.UploadStorage;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/gif"
    );

    private final ProfileService profileService;
    private final UserInterestService userInterestService;
    private final UserService userService;
    private final UploadStorage uploadStorage;

    public ProfileController(
        ProfileService profileService,
        UserInterestService userInterestService,
        UserService userService,
        UploadStorage uploadStorage
    ) {
        this.profileService = profileService;
        this.userInterestService = userInterestService;
        this.userService = userService;
        this.uploadStorage = uploadStorage;
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

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadAvatar(@CurrentUser User currentUser, @RequestPart("file") MultipartFile file) {
        validateAvatarFile(file);

        try {
            Files.createDirectories(uploadStorage.getAvatarDirectory());

            String extension = resolveImageExtension(file);
            String filename = currentUser.getId() + "-" + UUID.randomUUID() + extension;
            Path target = uploadStorage.getAvatarDirectory().resolve(filename).normalize();
            if (!target.startsWith(uploadStorage.getAvatarDirectory())) {
                throw new IllegalArgumentException("Avatar file path is invalid");
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String avatarUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(uploadStorage.getAvatarAccessPath(filename))
                .toUriString();
            return Result.success(avatarUrl);
        } catch (IOException exception) {
            throw new IllegalStateException("Avatar upload failed");
        }
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file cannot be empty");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("Avatar size cannot exceed 2 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Avatar must be JPG, PNG, WEBP, or GIF");
        }
    }

    private String resolveImageExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            int extensionIndex = originalFilename.lastIndexOf('.');
            if (extensionIndex >= 0 && extensionIndex < originalFilename.length() - 1) {
                return originalFilename.substring(extensionIndex).toLowerCase();
            }
        }

        return switch (file.getContentType()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".png";
        };
    }
}
