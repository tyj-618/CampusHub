package com.tyj.campushub.post;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public interface HotPostCache {

    Optional<List<PostHotItemResponse>> get(int limit, Long categoryId);

    void put(int limit, Long categoryId, List<PostHotItemResponse> hotPosts);

    default List<PostHotItemResponse> getOrLoad(int limit, Long categoryId, Supplier<List<PostHotItemResponse>> loader) {
        return get(limit, categoryId)
                .orElseGet(() -> {
                    List<PostHotItemResponse> hotPosts = loader.get();
                    put(limit, categoryId, hotPosts);
                    return hotPosts;
                });
    }
}
