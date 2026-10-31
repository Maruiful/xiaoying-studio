package com.xiaoying.task;

import com.xiaoying.config.ExecutorConfig;
import com.xiaoying.pipeline.TaskPipeline;
import com.xiaoying.pipeline.TaskStateRecorder;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 把任务交给线程池执行。必须在事务提交之后派发，否则工作线程可能读不到还没提交的任务记录。
 */
@Slf4j
@Component
public class TaskDispatcher {

    static final String BUSY_REASON = "系统繁忙，请稍后重试";

    private final TaskExecutor executor;
    private final TaskPipeline pipeline;
    private final TaskStateRecorder recorder;

    public TaskDispatcher(
            @Qualifier(ExecutorConfig.TASK_EXECUTOR) TaskExecutor executor,
            TaskPipeline pipeline,
            TaskStateRecorder recorder) {
        this.executor = executor;
        this.pipeline = pipeline;
        this.recorder = recorder;
    }

    /** 在当前事务提交后派发；事务回滚时不派发。必须在事务内调用。 */
    public void dispatchAfterCommit(long taskId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("dispatchAfterCommit 必须在事务内调用");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dispatch(taskId);
            }
        });
    }

    private void dispatch(long taskId) {
        try {
            executor.execute(() -> pipeline.run(taskId));
        } catch (RejectedExecutionException e) {
            log.warn("线程池已满，任务 {} 直接标为失败", taskId);
            recorder.failedBeforeStart(taskId, BUSY_REASON);
        }
    }
}
