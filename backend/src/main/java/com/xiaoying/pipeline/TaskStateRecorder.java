package com.xiaoying.pipeline;

import com.xiaoying.task.StepStatus;
import com.xiaoying.task.TaskMapper;
import com.xiaoying.task.TaskStage;
import com.xiaoying.task.TaskStepMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 记录流水线的状态变化。每个方法是一个短事务，同时更新 task 和 task_step，
 * 两张表要么一起变，要么都不变。事务里只有 SQL，没有等待或文件 IO。
 *
 * <p>所有方法返回是否更新成功。返回 false 说明任务状态已被改变（例如已是终态），调用方必须停止执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskStateRecorder {

    private final TaskMapper taskMapper;
    private final TaskStepMapper taskStepMapper;
    private final Clock clock;

    @Transactional
    public boolean stepStarted(long taskId, TaskStage stage) {
        Instant now = now();
        if (taskMapper.markRunning(taskId, stage, now) == 0) {
            log.warn("任务不在可执行状态，停止执行: stage={}", stage);
            return false;
        }
        taskStepMapper.insertRunning(taskId, stage, now);
        return true;
    }

    @Transactional
    public boolean stepSucceeded(long taskId, TaskStage stage, String message) {
        return finishStep(taskId, stage, StepStatus.SUCCEEDED, message);
    }

    /** 三步全部完成：最后一步的 task_step 和 task 一起变为 SUCCEEDED。 */
    @Transactional
    public boolean taskSucceeded(long taskId, String resultUrl) {
        Instant now = now();
        int steps = taskStepMapper.finish(taskId, TaskStage.ORGANIZE_RESULT, StepStatus.SUCCEEDED, resultUrl, now);
        int tasks = taskMapper.markSucceeded(taskId, resultUrl, now);
        return checkBoth(steps, tasks, TaskStage.ORGANIZE_RESULT);
    }

    /** 某一步失败：该步的 task_step 和 task 一起变为 FAILED，task.stage 停在这一步。 */
    @Transactional
    public boolean stepFailed(long taskId, TaskStage stage, String reason) {
        Instant now = now();
        int steps = taskStepMapper.finish(taskId, stage, StepStatus.FAILED, reason, now);
        int tasks = taskMapper.markFailedAtStage(taskId, stage, reason, now);
        return checkBoth(steps, tasks, stage);
    }

    /** 还没开始执行就失败（线程池拒绝）。 */
    @Transactional
    public boolean failedBeforeStart(long taskId, String reason) {
        return taskMapper.markFailedWhilePending(taskId, reason, now()) == 1;
    }

    private boolean finishStep(long taskId, TaskStage stage, StepStatus status, String message) {
        int steps = taskStepMapper.finish(taskId, stage, status, message, now());
        if (steps == 0) {
            log.warn("步骤记录不在 RUNNING 状态: stage={}", stage);
            return false;
        }
        return true;
    }

    /** 两张表都必须更新成功，否则回滚，避免 task 和 task_step 不一致。 */
    private boolean checkBoth(int steps, int tasks, TaskStage stage) {
        if (steps == 1 && tasks == 1) {
            return true;
        }
        log.warn("终态更新失败，任务状态已被改变: stage={}, steps={}, tasks={}", stage, steps, tasks);
        throw new StaleTaskStateException();
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }

    /** 用于触发事务回滚，由 TaskPipeline 捕获后停止执行。 */
    static class StaleTaskStateException extends RuntimeException {
        StaleTaskStateException() {
            super("任务状态已被改变", null, false, false);
        }
    }
}
