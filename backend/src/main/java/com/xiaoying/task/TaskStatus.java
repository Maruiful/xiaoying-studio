package com.xiaoying.task;

/** 任务整体状态。SUCCEEDED 和 FAILED 是终态，进入后不再改变。 */
public enum TaskStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED;
    }
}
