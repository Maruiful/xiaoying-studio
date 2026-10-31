package com.xiaoying.task;

import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TaskStepMapper {

    @Insert("""
            INSERT INTO task_step (task_id, stage, status, started_at)
            VALUES (#{taskId}, #{stage}, 'RUNNING', #{now})
            """)
    int insertRunning(@Param("taskId") long taskId, @Param("stage") TaskStage stage, @Param("now") Instant now);

    @Update("""
            UPDATE task_step SET status = #{status}, message = #{message}, finished_at = #{now}
            WHERE task_id = #{taskId} AND stage = #{stage} AND status = 'RUNNING'
            """)
    int finish(
            @Param("taskId") long taskId,
            @Param("stage") TaskStage stage,
            @Param("status") StepStatus status,
            @Param("message") String message,
            @Param("now") Instant now);

    @Update("""
            UPDATE task_step SET status = 'FAILED', message = #{message}, finished_at = #{now}
            WHERE status = 'RUNNING'
            """)
    int failAllRunning(@Param("message") String message, @Param("now") Instant now);

    @Select("""
            SELECT id, task_id, stage, status, message, started_at, finished_at
            FROM task_step WHERE task_id = #{taskId} ORDER BY id
            """)
    List<TaskStepEntity> findByTask(@Param("taskId") long taskId);
}
