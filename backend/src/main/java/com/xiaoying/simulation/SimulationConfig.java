package com.xiaoying.simulation;

import com.xiaoying.task.TaskStage;
import java.util.Map;

/**
 * 模拟生成器的配置：每个阶段的耗时，以及让哪个阶段失败。
 *
 * <p>任务提交时把当前配置存成快照（task.sim_config），执行时只读快照，运行中修改配置不影响已提交的任务。
 *
 * @param stageDelaysMs 每个阶段的模拟耗时（毫秒），三个阶段都必须有
 * @param failStage 让哪个阶段失败，null 表示全部成功
 */
public record SimulationConfig(Map<TaskStage, Long> stageDelaysMs, TaskStage failStage) {

    public SimulationConfig {
        stageDelaysMs = Map.copyOf(stageDelaysMs);
    }

    public long delayOf(TaskStage stage) {
        return stageDelaysMs.getOrDefault(stage, 0L);
    }

    public boolean shouldFail(TaskStage stage) {
        return stage == failStage;
    }
}
