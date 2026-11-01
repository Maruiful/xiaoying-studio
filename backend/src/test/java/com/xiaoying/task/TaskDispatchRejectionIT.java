package com.xiaoying.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.xiaoying.TestcontainersConfiguration;
import com.xiaoying.pipeline.TaskPipeline;
import com.xiaoying.pipeline.TaskStateRecorder;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TaskDispatchRejectionIT {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transactions;

    @Autowired
    TaskPipeline pipeline;

    @Autowired
    TaskStateRecorder recorder;

    @Test
    @DisplayName("事务提交后线程池拒绝任务时，失败状态持久化到数据库")
    void rejectedTaskIsPersistedAsFailed() {
        String userId = UUID.randomUUID().toString();
        TaskExecutor rejectingExecutor = command -> {
            throw new RejectedExecutionException("full");
        };
        TaskDispatcher dispatcher = new TaskDispatcher(rejectingExecutor, pipeline, recorder);

        Long taskId = transactions.execute(status -> {
            jdbc.update("INSERT INTO app_user (id, created_at) VALUES (?, UTC_TIMESTAMP(3))", userId);
            jdbc.update("""
                    INSERT INTO task (user_id, template_id, status, sim_config, created_at, updated_at)
                    VALUES (?, 'demo-retro-poster', 'PENDING', '{}', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                    """, userId);
            Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            dispatcher.dispatchAfterCommit(id);
            return id;
        });

        assertThat(jdbc.queryForObject("SELECT status FROM task WHERE id = ?", String.class, taskId))
                .isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("SELECT fail_reason FROM task WHERE id = ?", String.class, taskId))
                .isEqualTo(TaskDispatcher.BUSY_REASON);
    }
}
