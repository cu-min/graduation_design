package com.graduationdesign.newsrecommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.graduationdesign.newsrecommendation.dto.CommentCreateRequest;
import com.graduationdesign.newsrecommendation.entity.Comment;
import com.graduationdesign.newsrecommendation.entity.News;
import com.graduationdesign.newsrecommendation.entity.User;
import com.graduationdesign.newsrecommendation.exception.NotFoundException;
import com.graduationdesign.newsrecommendation.mapper.CommentMapper;
import com.graduationdesign.newsrecommendation.mapper.NewsMapper;
import com.graduationdesign.newsrecommendation.mapper.UserMapper;
import com.graduationdesign.newsrecommendation.service.CommentService;
import com.graduationdesign.newsrecommendation.vo.CommentReplyVO;
import com.graduationdesign.newsrecommendation.vo.CommentVO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final NewsMapper newsMapper;
    private final UserMapper userMapper;

    public CommentServiceImpl(NewsMapper newsMapper, UserMapper userMapper) {
        this.newsMapper = newsMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<CommentVO> listNewsComments(Long newsId, User currentUser) {
        getActiveNews(newsId);

        List<Comment> comments = list(new LambdaQueryWrapper<Comment>()
            .eq(Comment::getNewsId, newsId)
            .eq(Comment::getStatus, 1)
            .orderByDesc(Comment::getCreatedAt)
            .orderByDesc(Comment::getId));

        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, User> userMap = loadUsers(comments);
        Map<Long, List<Comment>> repliesByParentId = comments.stream()
            .filter(comment -> comment.getParentId() != null)
            .collect(Collectors.groupingBy(Comment::getParentId));

        return comments.stream()
            .filter(comment -> comment.getParentId() == null)
            .map(comment -> toCommentVO(comment, repliesByParentId.getOrDefault(comment.getId(), Collections.emptyList()), userMap, currentUser))
            .toList();
    }

    @Override
    @Transactional
    public void createNewsComment(Long newsId, Long userId, CommentCreateRequest request) {
        getActiveNews(newsId);

        Comment comment = new Comment();
        comment.setNewsId(newsId);
        comment.setUserId(userId);
        comment.setParentId(null);
        comment.setContent(request.getContent().trim());
        comment.setStatus(1);
        save(comment);

        newsMapper.update(
            null,
            new LambdaUpdateWrapper<News>()
                .eq(News::getId, newsId)
                .setSql("comment_count = comment_count + 1")
        );
    }

    @Override
    @Transactional
    public void replyToComment(Long commentId, Long userId, CommentCreateRequest request) {
        Comment parentComment = getActiveComment(commentId);
        if (parentComment.getParentId() != null) {
            throw new IllegalArgumentException("Only replies to top-level comments are supported");
        }

        getActiveNews(parentComment.getNewsId());

        Comment reply = new Comment();
        reply.setNewsId(parentComment.getNewsId());
        reply.setUserId(userId);
        reply.setParentId(parentComment.getId());
        reply.setContent(request.getContent().trim());
        reply.setStatus(1);
        save(reply);

        newsMapper.update(
            null,
            new LambdaUpdateWrapper<News>()
                .eq(News::getId, parentComment.getNewsId())
                .setSql("comment_count = comment_count + 1")
        );
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, User currentUser) {
        Comment comment = getActiveComment(commentId);
        boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
        if (!Objects.equals(comment.getUserId(), currentUser.getId()) && !isAdmin) {
            throw new IllegalArgumentException("You can only delete your own comments");
        }

        getActiveNews(comment.getNewsId());
        List<Comment> toDelete = new ArrayList<>();
        toDelete.add(comment);

        if (comment.getParentId() == null) {
            List<Comment> replies = list(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getParentId, commentId)
                .eq(Comment::getStatus, 1));
            toDelete.addAll(replies);
        }

        Set<Long> commentIds = toDelete.stream().map(Comment::getId).collect(Collectors.toSet());
        if (!commentIds.isEmpty()) {
            Comment deletedState = new Comment();
            deletedState.setStatus(0);
            update(deletedState, new LambdaQueryWrapper<Comment>().in(Comment::getId, commentIds));
            newsMapper.update(
                null,
                new LambdaUpdateWrapper<News>()
                    .eq(News::getId, comment.getNewsId())
                    .setSql("comment_count = GREATEST(comment_count - " + commentIds.size() + ", 0)")
            );
        }
    }

    private CommentVO toCommentVO(Comment comment, List<Comment> replies, Map<Long, User> userMap, User currentUser) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setNewsId(comment.getNewsId());
        vo.setUserId(comment.getUserId());
        vo.setNickname(resolveNickname(comment.getUserId(), userMap));
        vo.setContent(comment.getContent());
        vo.setCreatedAt(comment.getCreatedAt());
        vo.setCanDelete(canDelete(comment, currentUser));
        vo.setReplies(replies.stream()
            .sorted((left, right) -> left.getCreatedAt().compareTo(right.getCreatedAt()))
            .map(reply -> toReplyVO(reply, userMap, currentUser))
            .toList());
        return vo;
    }

    private CommentReplyVO toReplyVO(Comment comment, Map<Long, User> userMap, User currentUser) {
        CommentReplyVO vo = new CommentReplyVO();
        vo.setId(comment.getId());
        vo.setNewsId(comment.getNewsId());
        vo.setUserId(comment.getUserId());
        vo.setNickname(resolveNickname(comment.getUserId(), userMap));
        vo.setContent(comment.getContent());
        vo.setCreatedAt(comment.getCreatedAt());
        vo.setCanDelete(canDelete(comment, currentUser));
        return vo;
    }

    private boolean canDelete(Comment comment, User currentUser) {
        if (currentUser == null) {
            return false;
        }
        return Objects.equals(comment.getUserId(), currentUser.getId())
            || "ADMIN".equalsIgnoreCase(currentUser.getRole());
    }

    private String resolveNickname(Long userId, Map<Long, User> userMap) {
        User user = userMap.get(userId);
        if (user == null) {
            return "Unknown User";
        }
        return user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname();
    }

    private Map<Long, User> loadUsers(List<Comment> comments) {
        Set<Long> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        return userIds.isEmpty()
            ? Collections.emptyMap()
            : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Comment getActiveComment(Long commentId) {
        Comment comment = getById(commentId);
        if (comment == null || comment.getStatus() == null || comment.getStatus() != 1) {
            throw new NotFoundException("Comment does not exist");
        }
        return comment;
    }

    private News getActiveNews(Long newsId) {
        News news = newsMapper.selectById(newsId);
        if (news == null || news.getStatus() == null || news.getStatus() != 1) {
            throw new NotFoundException("News does not exist or has been taken offline");
        }
        return news;
    }
}
