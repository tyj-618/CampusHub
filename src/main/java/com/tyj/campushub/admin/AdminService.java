package com.tyj.campushub.admin;

import com.tyj.campushub.auth.CurrentUserService;
import com.tyj.campushub.common.ErrorCode;
import com.tyj.campushub.exception.BusinessException;
import com.tyj.campushub.user.UserProfile;
import com.tyj.campushub.user.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private static final int USER_STATUS_NORMAL = 0;
    private static final int USER_STATUS_DISABLED = 1;
    private static final int POST_STATUS_NORMAL = 0;
    private static final int POST_STATUS_HIDDEN = 2;

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    public AdminService(CurrentUserService currentUserService, UserRepository userRepository, AdminRepository adminRepository) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
    }

    public void hidePost(Long postId, String authorization) {
        requireAdmin(authorization);
        ensurePostExists(postId);
        adminRepository.updatePostStatus(postId, POST_STATUS_HIDDEN);
    }

    public void restorePost(Long postId, String authorization) {
        requireAdmin(authorization);
        ensurePostExists(postId);
        adminRepository.updatePostStatus(postId, POST_STATUS_NORMAL);
    }

    public void disableUser(Long userId, String authorization) {
        Long currentUserId = requireAdmin(authorization);
        if (currentUserId.equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能禁用当前登录的管理员账号");
        }

        ensureUserExists(userId);
        adminRepository.updateUserStatus(userId, USER_STATUS_DISABLED);
    }

    public void enableUser(Long userId, String authorization) {
        requireAdmin(authorization);
        ensureUserExists(userId);
        adminRepository.updateUserStatus(userId, USER_STATUS_NORMAL);
    }

    private Long requireAdmin(String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        UserProfile currentUser = userRepository.findProfileById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (currentUser.status() != USER_STATUS_NORMAL) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户已被禁用");
        }

        if (currentUser.role() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要管理员权限");
        }

        return currentUserId;
    }

    private void ensurePostExists(Long postId) {
        if (!adminRepository.existsPost(postId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
    }

    private void ensureUserExists(Long userId) {
        if (!adminRepository.existsUser(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
    }
}
