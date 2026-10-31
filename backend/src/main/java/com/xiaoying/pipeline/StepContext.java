package com.xiaoying.pipeline;

import com.xiaoying.simulation.SimulationConfig;
import com.xiaoying.task.TaskEntity;
import java.util.List;

/**
 * 执行步骤所需的上下文，执行前从数据库读出，执行过程中不变。
 *
 * @param assetIds 按上传位顺序排列
 * @param simulation 提交时的模拟配置快照
 */
public record StepContext(TaskEntity task, List<Long> assetIds, SimulationConfig simulation) {

    public long taskId() {
        return task.getId();
    }
}
