package com.xiaoying.pipeline;

import com.xiaoying.task.TaskStage;
import org.springframework.stereotype.Component;

/** 第二步：模拟生成。不调用真实 AI 服务，只按配置等待一段时间。 */
@Component
public class GenerateStep implements TaskStep {

    @Override
    public TaskStage stage() {
        return TaskStage.GENERATE;
    }

    @Override
    public String execute(StepContext context) throws InterruptedException {
        Simulator.simulate(context, stage());
        return "模拟生成完成";
    }
}
