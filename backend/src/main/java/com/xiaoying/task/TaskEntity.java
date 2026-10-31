package com.xiaoying.task;

import java.time.Instant;
import lombok.Data;

/** 对应 task 表。sim_config 存提交时的模拟配置快照（JSON 字符串）。 */
@Data
public class TaskEntity {
    private Long id;
    private String userId;
    private String templateId;
    private TaskStatus status;
    private TaskStage stage;
    private String resultUrl;
    private String failReason;
    private String simConfig;
    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant updatedAt;
}
