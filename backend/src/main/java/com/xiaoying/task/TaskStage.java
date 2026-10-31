package com.xiaoying.task;

/** 流水线的三个阶段，按声明顺序执行。 */
public enum TaskStage {
    CHECK_ASSETS("素材检查"),
    GENERATE("模拟生成"),
    ORGANIZE_RESULT("结果整理");

    private final String label;

    TaskStage(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
