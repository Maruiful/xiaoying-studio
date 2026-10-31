package com.xiaoying.task;

/** 提交成功只返回任务 ID，状态通过查询接口获取。 */
public record CreateTaskVO(long taskId) {}
