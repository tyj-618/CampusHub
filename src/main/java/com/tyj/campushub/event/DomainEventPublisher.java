package com.tyj.campushub.event;

public interface DomainEventPublisher {

    void publishCommentCreated(CommentCreatedEvent event);

    void publishPostLiked(PostLikedEvent event);
}
