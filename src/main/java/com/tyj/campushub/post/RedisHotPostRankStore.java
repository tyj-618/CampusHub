package com.tyj.campushub.post;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Component
@Profile("redis")
public class RedisHotPostRankStore implements HotPostRankStore {

    private static final String ALL_POSTS_KEY = "campushub:rank:post:hot:all";
    private static final String CATEGORY_KEY_PREFIX = "campushub:rank:post:hot:category:";

    private final StringRedisTemplate stringRedisTemplate;
    private final PostMapper postMapper;

    public RedisHotPostRankStore(StringRedisTemplate stringRedisTemplate, PostMapper postMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.postMapper = postMapper;
    }

    @Override
    public List<PostHotItemResponse> listHotPosts(int limit, Long categoryId, Supplier<List<PostHotItemResponse>> dbLoader) {
        String key = buildKey(categoryId);
        Set<String> postIdValues = stringRedisTemplate.opsForZSet().reverseRange(key, 0, limit - 1L);
        if (postIdValues == null || postIdValues.isEmpty()) {
            return reloadFromDatabase(key, dbLoader);
        }

        List<Long> postIds = parsePostIds(postIdValues);
        List<PostHotItemResponse> hotPosts = postMapper.findHotPostsByIds(postIds, categoryId)
                .stream()
                .map(PostHotItemResponse::from)
                .toList();

        if (hotPosts.size() != postIds.size()) {
            return reloadFromDatabase(key, dbLoader);
        }
        return hotPosts;
    }

    @Override
    public void increaseScore(Long postId, Long categoryId, double delta) {
        incrementIfPresent(ALL_POSTS_KEY, postId, delta);
        if (categoryId != null) {
            incrementIfPresent(buildCategoryKey(categoryId), postId, delta);
        }
    }

    @Override
    public void decreaseScore(Long postId, Long categoryId, double delta) {
        incrementIfPresent(ALL_POSTS_KEY, postId, -delta);
        if (categoryId != null) {
            incrementIfPresent(buildCategoryKey(categoryId), postId, -delta);
        }
    }

    @Override
    public void removePost(Long postId, Long categoryId) {
        removeIfPresent(ALL_POSTS_KEY, postId);
        if (categoryId != null) {
            removeIfPresent(buildCategoryKey(categoryId), postId);
        }
    }

    @Override
    public void moveCategory(Long postId, Long oldCategoryId, Long newCategoryId, double hotScore) {
        if (oldCategoryId != null) {
            removeIfPresent(buildCategoryKey(oldCategoryId), postId);
        }
        if (newCategoryId != null) {
            addIfPresent(buildCategoryKey(newCategoryId), postId, hotScore);
        }
    }

    private List<PostHotItemResponse> reloadFromDatabase(String key, Supplier<List<PostHotItemResponse>> dbLoader) {
        List<PostHotItemResponse> hotPosts = dbLoader.get();
        stringRedisTemplate.delete(key);
        for (PostHotItemResponse hotPost : hotPosts) {
            stringRedisTemplate.opsForZSet().add(key, hotPost.id().toString(), hotPost.hotScore());
        }
        return hotPosts;
    }

    private void incrementIfPresent(String key, Long postId, double delta) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            Double score = stringRedisTemplate.opsForZSet().incrementScore(key, postId.toString(), delta);
            if (score != null && score < 0) {
                stringRedisTemplate.opsForZSet().add(key, postId.toString(), 0);
            }
        }
    }

    private void addIfPresent(String key, Long postId, double score) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            stringRedisTemplate.opsForZSet().add(key, postId.toString(), score);
        }
    }

    private void removeIfPresent(String key, Long postId) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            stringRedisTemplate.opsForZSet().remove(key, postId.toString());
        }
    }

    private List<Long> parsePostIds(Set<String> postIdValues) {
        List<Long> postIds = new ArrayList<>(postIdValues.size());
        for (String postIdValue : postIdValues) {
            postIds.add(Long.valueOf(postIdValue));
        }
        return postIds;
    }

    private String buildKey(Long categoryId) {
        return categoryId == null ? ALL_POSTS_KEY : buildCategoryKey(categoryId);
    }

    private String buildCategoryKey(Long categoryId) {
        return CATEGORY_KEY_PREFIX + categoryId;
    }
}
