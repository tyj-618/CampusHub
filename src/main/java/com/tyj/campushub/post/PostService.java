package com.tyj.campushub.post;

import com.tyj.campushub.auth.CurrentUserService;
import com.tyj.campushub.common.ErrorCode;
import com.tyj.campushub.common.PageResponse;
import com.tyj.campushub.exception.BusinessException;
import com.tyj.campushub.user.UserProfile;
import com.tyj.campushub.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostService {

    private final CurrentUserService currentUserService;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final HotPostCache hotPostCache;
    private final ViewCountService viewCountService;

    public PostService(CurrentUserService currentUserService, PostMapper postMapper, UserMapper userMapper,
                       HotPostCache hotPostCache, ViewCountService viewCountService) {
        this.currentUserService = currentUserService;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.hotPostCache = hotPostCache;
        this.viewCountService = viewCountService;
    }

    @Transactional
    public CreatePostResponse createPost(String authorization, CreatePostRequest request) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        ensureEnabledCategory(request.categoryId());

        Long postId = postMapper.savePost(currentUserId, request);
        if (postId == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "帖子创建失败");
        }

        postMapper.savePostStat(postId);
        return new CreatePostResponse(postId);
    }

    public PageResponse<PostListItemResponse> listPosts(int page, int size, Long categoryId, String keyword, String sort) {
        if (categoryId != null) {
            ensureEnabledCategory(categoryId);
        }

        PageQueryResult<PostListItem> result = postMapper.findPosts(page, size, categoryId, keyword, sort);
        List<PostListItemResponse> records = result.records().stream()
                .map(PostListItemResponse::from)
                .toList();
        return PageResponse.of(page, size, result.total(), records);
    }

    public PostDetailResponse getPostDetail(Long postId, String authorization) {
        PostDetail postDetail = findNormalPost(postId);
        viewCountService.recordView(postId);

        boolean liked = currentUserService.findUserId(authorization)
                .map(userId -> postMapper.existsLike(postId, userId))
                .orElse(false);

        return PostDetailResponse.from(postDetail, liked);
    }

    public PageResponse<PostListItemResponse> listUserPosts(Long userId, int page, int size) {
        if (userMapper.findProfileById(userId).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        PageQueryResult<PostListItem> result = postMapper.findPostsByUserId(userId, page, size);
        List<PostListItemResponse> records = result.records().stream()
                .map(PostListItemResponse::from)
                .toList();
        return PageResponse.of(page, size, result.total(), records);
    }

    public List<PostHotItemResponse> listHotPosts(int limit, Long categoryId) {
        if (categoryId != null) {
            ensureEnabledCategory(categoryId);
        }

        return hotPostCache.getOrLoad(limit, categoryId, () -> loadHotPosts(limit, categoryId));
    }

    private List<PostHotItemResponse> loadHotPosts(int limit, Long categoryId) {
        return postMapper.findHotPosts(limit, categoryId)
                .stream()
                .map(PostHotItemResponse::from)
                .toList();
    }

    @Transactional
    public void updatePost(Long postId, String authorization, UpdatePostRequest request) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        PostDetail postDetail = findNormalPost(postId);
        ensureCanManagePost(currentUserId, postDetail.userId());
        ensureEnabledCategory(request.categoryId());
        postMapper.updatePost(postId, request);
    }

    public void deletePost(Long postId, String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        PostDetail postDetail = findNormalPost(postId);
        ensureCanManagePost(currentUserId, postDetail.userId());
        postMapper.softDeletePost(postId);
    }

    private PostDetail findNormalPost(Long postId) {
        PostDetail postDetail = postMapper.findDetailById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在"));

        if (postDetail.status() != 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }

        return postDetail;
    }

    private void ensureEnabledCategory(Long categoryId) {
        if (!postMapper.existsEnabledCategory(categoryId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在或已禁用");
        }
    }

    private void ensureCanManagePost(Long currentUserId, Long postAuthorId) {
        if (currentUserId.equals(postAuthorId)) {
            return;
        }

        UserProfile currentUser = userMapper.findProfileById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (currentUser.role() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能操作自己的帖子");
        }
    }
}
