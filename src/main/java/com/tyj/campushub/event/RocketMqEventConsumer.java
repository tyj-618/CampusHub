package com.tyj.campushub.event;

import com.tyj.campushub.notice.NoticeService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

public class RocketMqEventConsumer {

    @Component
    @Profile("rocketmq")
    @RocketMQMessageListener(
            topic = "${campushub.rocketmq.comment-topic}",
            consumerGroup = "campushub-comment-notice-consumer-group"
    )
    public static class CommentCreatedConsumer implements RocketMQListener<CommentCreatedEvent> {

        private final NoticeService noticeService;

        public CommentCreatedConsumer(NoticeService noticeService) {
            this.noticeService = noticeService;
        }

        @Override
        public void onMessage(CommentCreatedEvent event) {
            noticeService.createCommentNotice(event.receiverId(), event.senderId(), event.postId(), event.commentId());
        }
    }

    @Component
    @Profile("rocketmq")
    @RocketMQMessageListener(
            topic = "${campushub.rocketmq.like-topic}",
            consumerGroup = "campushub-like-notice-consumer-group"
    )
    public static class PostLikedConsumer implements RocketMQListener<PostLikedEvent> {

        private final NoticeService noticeService;

        public PostLikedConsumer(NoticeService noticeService) {
            this.noticeService = noticeService;
        }

        @Override
        public void onMessage(PostLikedEvent event) {
            noticeService.createLikeNotice(event.receiverId(), event.senderId(), event.postId());
        }
    }
}
