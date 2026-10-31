package com.xiaoying.task;

import java.time.Instant;
import lombok.Data;

/** 查询用的投影：task 关联 template 取标题。不含 sim_config 等内部字段。 */
@Data
public class TaskView {
    private Long id;
    private String templateId;
    private String templateTitle;
    private TaskStatus status;
    private TaskStage stage;
    private String resultUrl;
    private String failReason;
    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
}
