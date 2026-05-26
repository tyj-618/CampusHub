package com.tyj.campushub.event;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("rocketmq")
public class RocketMqDomainEventPublisher implements DomainEventPublisher {

    private final RocketMQTemplate rocketMQTemplate;
    private final String commentTopic;
    private final String likeTopic;

    public RocketMqDomainEventPublisher(
            RocketMQTemplate rocketMQTemplate,
            @Value("${campushub.rocketmq.comment-topic}") String commentTopic,
            @Value("${campushub.rocketmq.like-topic}") String likeTopic) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.commentTopic = commentTopic;
        this.likeTopic = likeTopic;
    }

    @Override
    public void publishCommentCreated(CommentCreatedEvent event) {
        rocketMQTemplate.convertAndSend(commentTopic, event);
    }

    @Override
    public void publishPostLiked(PostLikedEvent event) {
        rocketMQTemplate.convertAndSend(likeTopic, event);
    }
}
