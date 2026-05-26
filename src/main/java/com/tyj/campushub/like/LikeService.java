package com.tyj.campushub.like;

import com.tyj.campushub.auth.CurrentUserService;
import com.tyj.campushub.common.ErrorCode;
import com.tyj.campushub.event.DomainEventPublisher;
import com.tyj.campushub.event.PostLikedEvent;
import com.tyj.campushub.exception.BusinessException;
import com.tyj.campushub.post.PostDetail;
import com.tyj.campushub.post.PostRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

    private final CurrentUserService currentUserService;
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final DomainEventPublisher domainEventPublisher;

    public LikeService(CurrentUserService currentUserService, LikeRepository likeRepository, PostRepository postRepository,
                       DomainEventPublisher domainEventPublisher) {
        this.currentUserService = currentUserService;
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public LikeResponse likePost(Long postId, String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        PostDetail postDetail = findNormalPost(postId);

        LikeRecord likeRecord = likeRepository.findByPostIdAndUserId(postId, currentUserId).orElse(null);
        if (likeRecord != null && likeRecord.status() == 0) {
            return new LikeResponse(true, likeRepository.findLikeCount(postId));
        }

        if (likeRecord == null) {
            try {
                likeRepository.saveLike(postId, currentUserId);
            } catch (DuplicateKeyException exception) {
                likeRecord = likeRepository.findByPostIdAndUserId(postId, currentUserId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "点赞状态查询失败"));
                if (likeRecord.status() == 0) {
                    return new LikeResponse(true, likeRepository.findLikeCount(postId));
                }
                likeRepository.activateLike(likeRecord.id());
            }
        } else {
            likeRepository.activateLike(likeRecord.id());
        }

        likeRepository.increaseLikeCount(postId);
        domainEventPublisher.publishPostLiked(new PostLikedEvent(postDetail.userId(), currentUserId, postId));
        return new LikeResponse(true, likeRepository.findLikeCount(postId));
    }

    @Transactional
    public LikeResponse unlikePost(Long postId, String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        ensureNormalPost(postId);

        LikeRecord likeRecord = likeRepository.findByPostIdAndUserId(postId, currentUserId).orElse(null);
        if (likeRecord == null || likeRecord.status() == 1) {
            return new LikeResponse(false, likeRepository.findLikeCount(postId));
        }

        likeRepository.cancelLike(likeRecord.id());
        likeRepository.decreaseLikeCount(postId);
        return new LikeResponse(false, likeRepository.findLikeCount(postId));
    }

    public LikeStatusResponse getLikeStatus(Long postId, String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        ensureNormalPost(postId);

        boolean liked = likeRepository.findByPostIdAndUserId(postId, currentUserId)
                .map(likeRecord -> likeRecord.status() == 0)
                .orElse(false);

        return new LikeStatusResponse(liked);
    }

    private void ensureNormalPost(Long postId) {
        if (!likeRepository.existsNormalPost(postId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
    }

    private PostDetail findNormalPost(Long postId) {
        PostDetail postDetail = postRepository.findDetailById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在"));

        if (postDetail.status() != 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }

        return postDetail;
    }
}
