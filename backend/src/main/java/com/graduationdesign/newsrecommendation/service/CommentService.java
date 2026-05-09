package com.graduationdesign.newsrecommendation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduationdesign.newsrecommendation.dto.CommentCreateRequest;
import com.graduationdesign.newsrecommendation.entity.Comment;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.vo.CommentVO;
import java.util.List;

public interface CommentService extends IService<Comment> {

    List<CommentVO> listNewsComments(Long newsId, User currentUser);

    void createNewsComment(Long newsId, Long userId, CommentCreateRequest request);

    void replyToComment(Long commentId, Long userId, CommentCreateRequest request);

    void deleteComment(Long commentId, User currentUser);
}
