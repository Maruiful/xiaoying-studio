package com.xiaoying.task;

import java.time.Instant;

/**
 * 任务查询结果。status 是整体状态，stage 是当前（或失败时所在的）阶段，PENDING 时为 null。
 *
 * @param resultUrl 仅 SUCCEEDED 时有值
 * @param failReason 仅 FAILED 时有值
 */
public record TaskVO(
        long id,
        String templateId,
        String templateTitle,
        TaskStatus status,
        TaskStage stage,
        String resultUrl,
        String failReason,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt) {

    static TaskVO from(TaskView v) {
        return new TaskVO(
                v.getId(),
                v.getTemplateId(),
                v.getTemplateTitle(),
                v.getStatus(),
                v.getStage(),
                v.getResultUrl(),
                v.getFailReason(),
                v.getCreatedAt(),
                v.getStartedAt(),
                v.getFinishedAt());
    }
}
