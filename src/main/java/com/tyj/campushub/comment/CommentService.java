package com.tyj.campushub.comment;

import com.tyj.campushub.auth.CurrentUserService;
import com.tyj.campushub.common.ErrorCode;
import com.tyj.campushub.common.PageResponse;
import com.tyj.campushub.event.CommentCreatedEvent;
import com.tyj.campushub.event.DomainEventPublisher;
import com.tyj.campushub.exception.BusinessException;
import com.tyj.campushub.post.PageQueryResult;
import com.tyj.campushub.post.PostDetail;
import com.tyj.campushub.post.PostMapper;
import com.tyj.campushub.user.UserProfile;
import com.tyj.campushub.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CurrentUserService currentUserService;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final DomainEventPublisher domainEventPublisher;

    public CommentService(CurrentUserService currentUserService, CommentMapper commentMapper, UserMapper userMapper,
                          PostMapper postMapper, DomainEventPublisher domainEventPublisher) {
        this.currentUserService = currentUserService;
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
        this.postMapper = postMapper;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public CreateCommentResponse createComment(Long postId, String authorization, CreateCommentRequest request) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        PostDetail postDetail = findNormalPost(postId);

        Long commentId = commentMapper.saveComment(postId, currentUserId, request.content());
        if (commentId == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "评论创建失败");
        }

        commentMapper.increaseCommentCount(postId);
        domainEventPublisher.publishCommentCreated(new CommentCreatedEvent(postDetail.userId(), currentUserId, postId, commentId));
        return new CreateCommentResponse(commentId);
    }

    public PageResponse<CommentResponse> listPostComments(Long postId, int page, int size) {
        ensureNormalPost(postId);

        PageQueryResult<CommentPageItem> result = commentMapper.findCommentsByPostId(postId, page, size);
        List<CommentResponse> records = result.records().stream()
                .map(CommentResponse::from)
                .toList();

        return PageResponse.of(page, size, result.total(), records);
    }

    @Transactional
    public void deleteComment(Long commentId, String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        CommentDetail commentDetail = findNormalComment(commentId);
        ensureCanDeleteComment(currentUserId, commentDetail);

        commentMapper.softDeleteComment(commentId);
        commentMapper.decreaseCommentCount(commentDetail.postId());
    }

    public PageResponse<MyCommentResponse> listMyComments(String authorization, int page, int size) {
        Long currentUserId = currentUserService.requireUserId(authorization);

        PageQueryResult<MyCommentItem> result = commentMapper.findCommentsByUserId(currentUserId, page, size);
        List<MyCommentResponse> records = result.records().stream()
                .map(MyCommentResponse::from)
                .toList();

        return PageResponse.of(page, size, result.total(), records);
    }

    private void ensureNormalPost(Long postId) {
        if (!commentMapper.existsNormalPost(postId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
    }

    private PostDetail findNormalPost(Long postId) {
        PostDetail postDetail = postMapper.findDetailById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在"));

        if (postDetail.status() != 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }

        return postDetail;
    }

    private CommentDetail findNormalComment(Long commentId) {
        CommentDetail commentDetail = commentMapper.findDetailById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "评论不存在"));

        if (commentDetail.status() != 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }

        return commentDetail;
    }

    private void ensureCanDeleteComment(Long currentUserId, CommentDetail commentDetail) {
        if (currentUserId.equals(commentDetail.userId()) || currentUserId.equals(commentDetail.postAuthorId())) {
            return;
        }

        UserProfile currentUser = userMapper.findProfileById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (currentUser.role() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能删除自己的评论或自己帖子下的评论");
        }
    }
}
