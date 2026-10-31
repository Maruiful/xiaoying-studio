package com.xiaoying.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutorConfig {

    public static final String TASK_EXECUTOR = "taskPipelineExecutor";

    /**
     * 有界线程池。队列满时抛出 RejectedExecutionException（AbortPolicy），由 TaskDispatcher 把任务标为失败；
     * 不用 CallerRunsPolicy，否则会在提交接口的请求线程里同步执行整条流水线。
     */
    @Bean(name = TASK_EXECUTOR, destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor taskPipelineExecutor(ExecutorProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.coreSize());
        executor.setMaxPoolSize(props.maxSize());
        executor.setQueueCapacity(props.queueCapacity());
        executor.setThreadNamePrefix("task-pipeline-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // 关闭时中断执行中的任务；被中断的任务会记为失败（处理被中断）
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
