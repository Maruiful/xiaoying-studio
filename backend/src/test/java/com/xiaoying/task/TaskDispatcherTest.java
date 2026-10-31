package com.xiaoying.task;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.xiaoying.pipeline.TaskPipeline;
import com.xiaoying.pipeline.TaskStateRecorder;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class TaskDispatcherTest {

    private final TaskExecutor executor = mock(TaskExecutor.class);
    private final TaskPipeline pipeline = mock(TaskPipeline.class);
    private final TaskStateRecorder recorder = mock(TaskStateRecorder.class);
    private final TaskDispatcher dispatcher = new TaskDispatcher(executor, pipeline, recorder);

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("事务提交前不派发，提交后才交给线程池")
    void dispatchesOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        dispatcher.dispatchAfterCommit(1L);
        verify(executor, never()).execute(any());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(executor).execute(any());
    }

    @Test
    @DisplayName("事务回滚时不派发")
    void doesNotDispatchOnRollback() {
        TransactionSynchronizationManager.initSynchronization();

        dispatcher.dispatchAfterCommit(1L);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(executor, never()).execute(any());
    }

    @Test
    @DisplayName("线程池已满时，任务直接标为失败")
    void marksTaskFailedWhenPoolIsFull() {
        doThrow(new RejectedExecutionException("full")).when(executor).execute(any());
        TransactionSynchronizationManager.initSynchronization();

        dispatcher.dispatchAfterCommit(9L);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(recorder).failedBeforeStart(9L, TaskDispatcher.BUSY_REASON);
    }

    @Test
    @DisplayName("不在事务内调用时报错，避免任务在提交前被执行")
    void requiresActiveTransaction() {
        assertThatThrownBy(() -> dispatcher.dispatchAfterCommit(1L)).isInstanceOf(IllegalStateException.class);
    }
}
