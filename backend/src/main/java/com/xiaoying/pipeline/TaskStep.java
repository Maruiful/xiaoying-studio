package com.xiaoying.pipeline;

import com.xiaoying.task.TaskStage;

/**
 * 流水线中的一步。实现类只做这一步自己的事，状态记录（task / task_step）统一由 TaskPipeline 负责。
 *
 * <p>失败时抛出 StepFailedException（给用户看的原因）；其他异常按内部错误处理。
 */
public interface TaskStep {

    TaskStage stage();

    /**
     * @return 这一步的产出说明，写入 task_step.message；结果整理步骤返回结果地址
     */
    String execute(StepContext context) throws InterruptedException;
}
