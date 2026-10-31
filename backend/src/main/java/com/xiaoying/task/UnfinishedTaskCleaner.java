package com.xiaoying.task;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 启动时把上次进程留下的 PENDING / RUNNING 任务标为失败。
 *
 * <p>任务只在内存线程池中执行，进程退出后不会继续；题目不要求重启后恢复执行，所以统一标为失败，
 * 避免页面上出现永远处理中的任务。只适用于单实例部署：多实例时会误伤其他实例正在执行的任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnfinishedTaskCleaner {

    static final String RESTART_REASON = "服务重启，处理被中断";

    private final TaskMapper taskMapper;
    private final TaskStepMapper taskStepMapper;
    private final Clock clock;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void failUnfinishedTasks() {
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
        int steps = taskStepMapper.failAllRunning(RESTART_REASON, now);
        int tasks = taskMapper.failAllUnfinished(RESTART_REASON, now);
        if (tasks > 0) {
            log.warn("启动时发现 {} 个未完成的任务（{} 个执行中的步骤），已标为失败", tasks, steps);
        }
    }
}
