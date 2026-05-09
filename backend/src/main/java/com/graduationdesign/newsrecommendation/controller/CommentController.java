package com.graduationdesign.newsrecommendation.controller;

import com.graduationdesign.newsrecommendation.common.Result;
import com.graduationdesign.newsrecommendation.dto.CommentCreateRequest;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.security.CurrentUser;
import com.graduationdesign.newsrecommendation.service.CommentService;
import com.graduationdesign.newsrecommendation.vo.CommentVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/news/{id}/comments")
    public Result<List<CommentVO>> listNewsComments(@PathVariable Long id, @CurrentUser User currentUser) {
        return Result.success(commentService.listNewsComments(id, currentUser));
    }

    @PostMapping("/news/{id}/comments")
    public Result<Void> createNewsComment(
        @PathVariable Long id,
        @CurrentUser User currentUser,
        @Valid @RequestBody CommentCreateRequest request
    ) {
        commentService.createNewsComment(id, currentUser.getId(), request);
        return Result.success();
    }

    @PostMapping("/comments/{commentId}/replies")
    public Result<Void> replyToComment(
        @PathVariable Long commentId,
        @CurrentUser User currentUser,
        @Valid @RequestBody CommentCreateRequest request
    ) {
        commentService.replyToComment(commentId, currentUser.getId(), request);
        return Result.success();
    }

    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId, @CurrentUser User currentUser) {
        commentService.deleteComment(commentId, currentUser);
        return Result.success();
    }
}
