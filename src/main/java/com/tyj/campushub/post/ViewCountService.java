package com.tyj.campushub.post;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

@Service
public class ViewCountService implements InitializingBean, DisposableBean {

    private final ViewCountMapper viewCountMapper;
    private final long flushIntervalSeconds;
    private final Map<Long, LongAdder> pendingViewCounts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ViewCountService(
            ViewCountMapper viewCountMapper,
            @Value("${campushub.view-count.flush-interval-seconds:10}") long flushIntervalSeconds) {
        this.viewCountMapper = viewCountMapper;
        this.flushIntervalSeconds = flushIntervalSeconds;
    }

    public void recordView(Long postId) {
        pendingViewCounts.computeIfAbsent(postId, key -> new LongAdder()).increment();
    }

    public void flushPendingViews() {
        for (Map.Entry<Long, LongAdder> entry : pendingViewCounts.entrySet()) {
            long delta = entry.getValue().sumThenReset();
            if (delta > 0) {
                viewCountMapper.increaseViewCount(entry.getKey(), delta);
            }
        }
    }

    @Override
    public void afterPropertiesSet() {
        scheduler.scheduleWithFixedDelay(
                this::flushSafely,
                flushIntervalSeconds,
                flushIntervalSeconds,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void destroy() {
        scheduler.shutdown();
        flushSafely();
    }

    private void flushSafely() {
        try {
            flushPendingViews();
        } catch (Exception ignored) {
            // The next scheduled flush will retry accumulated views.
        }
    }
}
