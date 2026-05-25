package com.tyj.campushub.user;

import com.tyj.campushub.auth.CurrentUserService;
import com.tyj.campushub.common.ErrorCode;
import com.tyj.campushub.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public UserService(CurrentUserService currentUserService, UserRepository userRepository) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    public UserProfileResponse getCurrentUser(String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        UserProfile userProfile = findExistingUser(currentUserId);
        return UserProfileResponse.from(userProfile);
    }

    public UserProfileResponse updateCurrentUser(String authorization, UpdateUserProfileRequest request) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        UserProfile oldProfile = findExistingUser(currentUserId);

        String nickname = cleanOrDefault(request.nickname(), oldProfile.nickname());
        String avatarUrl = cleanOrDefault(request.avatarUrl(), oldProfile.avatarUrl());
        String bio = cleanOrDefault(request.bio(), oldProfile.bio());

        if (nickname == null || nickname.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "昵称不能为空");
        }

        userRepository.updateProfile(currentUserId, nickname, avatarUrl, bio);

        UserProfile updatedProfile = findExistingUser(currentUserId);
        return UserProfileResponse.from(updatedProfile);
    }

    public PublicUserProfileResponse getPublicUserProfile(Long userId) {
        UserProfile userProfile = findExistingUser(userId);
        long postCount = userRepository.countNormalPostsByUserId(userId);
        long commentCount = userRepository.countNormalCommentsByUserId(userId);

        return new PublicUserProfileResponse(
                userProfile.id(),
                userProfile.username(),
                userProfile.nickname(),
                userProfile.avatarUrl(),
                userProfile.bio(),
                postCount,
                commentCount,
                userProfile.createdAt()
        );
    }

    private UserProfile findExistingUser(Long userId) {
        return userRepository.findProfileById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    }

    private String cleanOrDefault(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        String cleanedValue = value.trim();
        return cleanedValue.isEmpty() ? null : cleanedValue;
    }
}
