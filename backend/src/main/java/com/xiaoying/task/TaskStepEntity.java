package com.xiaoying.task;

import java.time.Instant;
import lombok.Data;

/** 对应 task_step 表：每个任务的每个阶段一条记录，记录该步的执行边界。 */
@Data
public class TaskStepEntity {
    private Long id;
    private Long taskId;
    private TaskStage stage;
    private StepStatus status;
    private String message;
    private Instant startedAt;
    private Instant finishedAt;
}
