package com.tyj.campushub.notice;

public record CreateNoticeCommand(
        Long receiverId,
        Long senderId,
        Long postId,
        Long commentId,
        Integer type,
        String content
) {
}
