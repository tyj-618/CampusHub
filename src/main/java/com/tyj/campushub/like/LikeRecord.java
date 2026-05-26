package com.tyj.campushub.like;

public record LikeRecord(
        Long id,
        Long postId,
        Long userId,
        Integer status
) {
}
