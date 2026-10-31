package com.xiaoying.pipeline;

import com.xiaoying.pipeline.TaskStateRecorder.StaleTaskStateException;
import com.xiaoying.simulation.SimulationService;
import com.xiaoying.task.TaskEntity;
import com.xiaoying.task.TaskMapper;
import com.xiaoying.task.TaskStage;
import com.xiaoying.task.TaskStatus;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * 按 TaskStage 的声明顺序依次执行三个步骤。
 *
 * <p>每一步的执行边界：stepStarted（task 进入该阶段 + 插入 task_step）→ 执行 → stepSucceeded 或 stepFailed。
 * 任何一步失败都会停止，后续步骤不会执行。状态更新返回 false 时也停止，不覆盖已有状态。
 *
 * <p>run() 在线程池的工作线程中调用，本身不抛出异常。
 */
@Slf4j
@Component
public class TaskPipeline {

    static final String INTERNAL_ERROR_REASON = "处理时发生内部错误";
    static final String INTERRUPTED_REASON = "处理被中断";

    private final List<TaskStep> steps;
    private final TaskStateRecorder recorder;
    private final TaskMapper taskMapper;
    private final SimulationService simulationService;

    public TaskPipeline(
            List<TaskStep> steps,
            TaskStateRecorder recorder,
            TaskMapper taskMapper,
            SimulationService simulationService) {
        this.steps = sortAndValidate(steps);
        this.recorder = recorder;
        this.taskMapper = taskMapper;
        this.simulationService = simulationService;
    }

    public void run(long taskId) {
        MDC.put("taskId", String.valueOf(taskId));
        try {
            StepContext context = loadContext(taskId);
            if (context == null) {
                return;
            }
            for (TaskStep step : steps) {
                if (!runStep(context, step)) {
                    return;
                }
            }
        } catch (RuntimeException e) {
            // loadContext 或状态记录本身失败（例如数据库不可用）。任务会留在 PENDING/RUNNING，下次启动时被标为失败
            log.error("任务执行异常", e);
        } finally {
            MDC.remove("taskId");
        }
    }

    /** @return 是否继续执行下一步 */
    private boolean runStep(StepContext context, TaskStep step) {
        long taskId = context.taskId();
        TaskStage stage = step.stage();
        if (!recorder.stepStarted(taskId, stage)) {
            return false;
        }
        log.info("开始 {}", stage);
        long start = System.nanoTime();
        try {
            String output = step.execute(context);
            long ms = (System.nanoTime() - start) / 1_000_000;
            boolean last = stage == TaskStage.ORGANIZE_RESULT;
            boolean ok = last ? recorder.taskSucceeded(taskId, output) : recorder.stepSucceeded(taskId, stage, output);
            log.info("完成 {}，耗时 {}ms{}", stage, ms, last ? "，任务成功" : "");
            return ok && !last;
        } catch (StepFailedException e) {
            log.info("{} 失败: {}", stage, e.reason());
            failQuietly(taskId, stage, e.reason());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("{} 被中断", stage);
            failQuietly(taskId, stage, INTERRUPTED_REASON);
        } catch (StaleTaskStateException e) {
            // 状态已被改变，什么都不做
        } catch (RuntimeException e) {
            log.error("{} 发生未预期的异常", stage, e);
            failQuietly(taskId, stage, INTERNAL_ERROR_REASON);
        }
        return false;
    }

    private void failQuietly(long taskId, TaskStage stage, String reason) {
        try {
            recorder.stepFailed(taskId, stage, reason);
        } catch (StaleTaskStateException e) {
            // 状态已被改变，什么都不做
        }
    }

    private StepContext loadContext(long taskId) {
        TaskEntity task = taskMapper.findById(taskId);
        if (task == null || task.getStatus() != TaskStatus.PENDING) {
            log.warn("任务不存在或不是 PENDING，跳过: status={}", task == null ? null : task.getStatus());
            return null;
        }
        return new StepContext(
                task, taskMapper.findAssetIds(taskId), simulationService.fromSnapshot(task.getSimConfig()));
    }

    /** 每个阶段必须正好有一个实现，按 TaskStage 的声明顺序排列。 */
    private static List<TaskStep> sortAndValidate(List<TaskStep> steps) {
        Set<TaskStage> stages = steps.stream().map(TaskStep::stage).collect(Collectors.toSet());
        if (steps.size() != TaskStage.values().length || !stages.equals(EnumSet.allOf(TaskStage.class))) {
            throw new IllegalStateException("每个阶段必须正好有一个 TaskStep 实现，实际: "
                    + steps.stream().map(TaskStep::stage).toList()
                    + "，期望: " + Arrays.toString(TaskStage.values()));
        }
        return steps.stream().sorted(Comparator.comparing(TaskStep::stage)).toList();
    }
}
