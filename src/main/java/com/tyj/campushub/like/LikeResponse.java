package com.tyj.campushub.like;

public record LikeResponse(
        boolean liked,
        int likeCount
) {
}
