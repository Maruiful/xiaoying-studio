package com.xiaoying.simulation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/** 所有字段可选，未给出的沿用当前值。耗时上限 60 秒，避免误填导致任务长时间占用线程。 */
public record UpdateSimulationRequest(
        @Min(value = 0, message = "耗时不能为负数") @Max(value = 60000, message = "耗时不能超过 60000 毫秒")
        Long checkAssetsMs,

        @Min(value = 0, message = "耗时不能为负数") @Max(value = 60000, message = "耗时不能超过 60000 毫秒")
        Long generateMs,

        @Min(value = 0, message = "耗时不能为负数") @Max(value = 60000, message = "耗时不能超过 60000 毫秒")
        Long organizeResultMs,

        @Pattern(
                regexp = "(?i)NONE|CHECK_ASSETS|GENERATE|ORGANIZE_RESULT",
                message = "只能是 NONE、CHECK_ASSETS、GENERATE、ORGANIZE_RESULT")
        String failStage) {}
