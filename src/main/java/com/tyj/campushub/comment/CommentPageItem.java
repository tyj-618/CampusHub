package com.tyj.campushub.comment;

import java.time.LocalDateTime;

public record CommentPageItem(
        Long id,
        Long postId,
        Long userId,
        String content,
        String authorNickname,
        String authorAvatarUrl,
        LocalDateTime createdAt
) {
}
