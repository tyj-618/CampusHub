package com.tyj.campushub.event;

public record PostLikedEvent(
        Long receiverId,
        Long senderId,
        Long postId
) {
}
