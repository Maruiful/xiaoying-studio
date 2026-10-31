package com.xiaoying.simulation;

import com.xiaoying.task.TaskStage;

/** 开发接口返回的当前模拟配置。failStage 为 "NONE" 表示全部成功。 */
public record SimulationVO(long checkAssetsMs, long generateMs, long organizeResultMs, String failStage) {

    static SimulationVO from(SimulationConfig c) {
        return new SimulationVO(
                c.delayOf(TaskStage.CHECK_ASSETS),
                c.delayOf(TaskStage.GENERATE),
                c.delayOf(TaskStage.ORGANIZE_RESULT),
                c.failStage() == null ? "NONE" : c.failStage().name());
    }
}
