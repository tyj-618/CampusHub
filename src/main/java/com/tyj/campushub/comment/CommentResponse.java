package com.tyj.campushub.comment;

import com.tyj.campushub.post.PostAuthorResponse;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        String content,
        PostAuthorResponse author,
        LocalDateTime createdAt
) {

    public static CommentResponse from(CommentPageItem item) {
        return new CommentResponse(
                item.id(),
                item.content(),
                new PostAuthorResponse(item.userId(), item.authorNickname(), item.authorAvatarUrl()),
                item.createdAt()
        );
    }
}
