package com.tyj.campushub.event;

public record CommentCreatedEvent(
        Long receiverId,
        Long senderId,
        Long postId,
        Long commentId
) {
}
