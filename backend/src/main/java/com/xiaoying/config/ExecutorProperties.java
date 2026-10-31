package com.xiaoying.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 执行任务流水线的线程池，对应 app.executor.*。队列满时新任务直接标为失败。
 *
 * <p>ThreadPoolExecutor 只有在队列满后才会创建超出 coreSize 的线程，所以同时执行的任务数由 coreSize 决定，
 * 默认让 coreSize 与 maxSize 相同。
 */
@ConfigurationProperties("app.executor")
public record ExecutorProperties(
        @DefaultValue("4") int coreSize,
        @DefaultValue("4") int maxSize,
        @DefaultValue("100") int queueCapacity) {}
