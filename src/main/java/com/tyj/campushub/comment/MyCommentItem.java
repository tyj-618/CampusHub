package com.tyj.campushub.comment;

import java.time.LocalDateTime;

public record MyCommentItem(
        Long id,
        Long postId,
        String postTitle,
        String content,
        LocalDateTime createdAt
) {
}
