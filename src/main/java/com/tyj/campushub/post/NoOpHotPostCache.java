package com.tyj.campushub.post;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Component
@Profile("!redis")
public class NoOpHotPostCache implements HotPostCache {

    @Override
    public Optional<List<PostHotItemResponse>> get(int limit, Long categoryId) {
        return Optional.empty();
    }

    @Override
    public void put(int limit, Long categoryId, List<PostHotItemResponse> hotPosts) {
    }

    @Override
    public List<PostHotItemResponse> getOrLoad(int limit, Long categoryId, Supplier<List<PostHotItemResponse>> loader) {
        return loader.get();
    }
}
