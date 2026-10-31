package com.xiaoying.pipeline;

import com.xiaoying.task.TaskStage;

/** 按快照中的配置模拟耗时和失败。三个步骤共用。 */
final class Simulator {

    private Simulator() {}

    /** 等待该阶段配置的耗时；如果配置要求该阶段失败，等待后抛出 StepFailedException。 */
    static void simulate(StepContext context, TaskStage stage) throws InterruptedException {
        long delay = context.simulation().delayOf(stage);
        if (delay > 0) {
            Thread.sleep(delay);
        }
        if (context.simulation().shouldFail(stage)) {
            throw new StepFailedException(stage.label() + "失败（模拟失败）");
        }
    }
}
