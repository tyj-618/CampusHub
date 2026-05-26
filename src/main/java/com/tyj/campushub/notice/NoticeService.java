package com.tyj.campushub.notice;

import com.tyj.campushub.auth.CurrentUserService;
import com.tyj.campushub.common.ErrorCode;
import com.tyj.campushub.common.PageResponse;
import com.tyj.campushub.exception.BusinessException;
import com.tyj.campushub.post.PageQueryResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeService {

    public static final int TYPE_COMMENT = 1;
    public static final int TYPE_LIKE = 2;

    private final CurrentUserService currentUserService;
    private final NoticeRepository noticeRepository;

    public NoticeService(CurrentUserService currentUserService, NoticeRepository noticeRepository) {
        this.currentUserService = currentUserService;
        this.noticeRepository = noticeRepository;
    }

    public void createCommentNotice(Long receiverId, Long senderId, Long postId, Long commentId) {
        if (receiverId.equals(senderId)) {
            return;
        }

        noticeRepository.saveNotice(new CreateNoticeCommand(
                receiverId,
                senderId,
                postId,
                commentId,
                TYPE_COMMENT,
                "你的帖子收到了新的评论"
        ));
    }

    public void createLikeNotice(Long receiverId, Long senderId, Long postId) {
        if (receiverId.equals(senderId)) {
            return;
        }

        noticeRepository.saveNotice(new CreateNoticeCommand(
                receiverId,
                senderId,
                postId,
                null,
                TYPE_LIKE,
                "你的帖子收到了新的点赞"
        ));
    }

    public PageResponse<NoticeResponse> listNotices(String authorization, int page, int size, Integer readStatus) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        validateReadStatus(readStatus);

        PageQueryResult<NoticeItem> result = noticeRepository.findNoticesByReceiverId(currentUserId, page, size, readStatus);
        List<NoticeResponse> records = result.records().stream()
                .map(NoticeResponse::from)
                .toList();

        return PageResponse.of(page, size, result.total(), records);
    }

    public UnreadNoticeCountResponse countUnreadNotices(String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        return new UnreadNoticeCountResponse(noticeRepository.countUnreadByReceiverId(currentUserId));
    }

    public void markNoticeRead(Long noticeId, String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        int updatedCount = noticeRepository.markRead(noticeId, currentUserId);

        if (updatedCount == 0 && !noticeRepository.existsByIdAndReceiverId(noticeId, currentUserId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
        }
    }

    public UpdateNoticeCountResponse markAllNoticesRead(String authorization) {
        Long currentUserId = currentUserService.requireUserId(authorization);
        int updatedCount = noticeRepository.markAllRead(currentUserId);
        return new UpdateNoticeCountResponse(updatedCount);
    }

    private void validateReadStatus(Integer readStatus) {
        if (readStatus != null && readStatus != 0 && readStatus != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "通知阅读状态只能是 0 或 1");
        }
    }
}
