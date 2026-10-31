package com.xiaoying.task;

import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 简单的单表 SQL 用注解，批量插入和带关联的查询在 resources/mapper/TaskMapper.xml。
 *
 * <p>所有状态更新都带前置状态条件，返回影响行数。返回 0 说明状态已经被改变，调用方不能继续往下执行。
 */
@Mapper
public interface TaskMapper {

    // ---- 写入 ----

    @Insert("""
            INSERT INTO task (user_id, template_id, status, sim_config, created_at, updated_at)
            VALUES (#{userId}, #{templateId}, 'PENDING', #{simConfig}, #{createdAt}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TaskEntity task);

    /** XML：foreach 批量插入，slot_index 取列表下标。 */
    int insertTaskAssets(@Param("taskId") long taskId, @Param("assetIds") List<Long> assetIds);

    // ---- 状态流转（都带前置状态条件） ----

    /** 进入某个阶段。只允许从 PENDING 或 RUNNING 进入，终态的任务不会被改回 RUNNING。 */
    @Update("""
            UPDATE task SET status = 'RUNNING', stage = #{stage},
                   started_at = COALESCE(started_at, #{now}), updated_at = #{now}
            WHERE id = #{id} AND status IN ('PENDING', 'RUNNING')
            """)
    int markRunning(@Param("id") long id, @Param("stage") TaskStage stage, @Param("now") Instant now);

    @Update("""
            UPDATE task SET status = 'SUCCEEDED', result_url = #{resultUrl}, finished_at = #{now}, updated_at = #{now}
            WHERE id = #{id} AND status = 'RUNNING' AND stage = 'ORGANIZE_RESULT'
            """)
    int markSucceeded(@Param("id") long id, @Param("resultUrl") String resultUrl, @Param("now") Instant now);

    /** 执行中某一步失败。stage 保持在失败的那一步。 */
    @Update("""
            UPDATE task SET status = 'FAILED', fail_reason = #{reason}, finished_at = #{now}, updated_at = #{now}
            WHERE id = #{id} AND status = 'RUNNING' AND stage = #{stage}
            """)
    int markFailedAtStage(
            @Param("id") long id,
            @Param("stage") TaskStage stage,
            @Param("reason") String reason,
            @Param("now") Instant now);

    /** 还没开始执行就失败（线程池拒绝）。 */
    @Update("""
            UPDATE task SET status = 'FAILED', fail_reason = #{reason}, finished_at = #{now}, updated_at = #{now}
            WHERE id = #{id} AND status = 'PENDING'
            """)
    int markFailedWhilePending(@Param("id") long id, @Param("reason") String reason, @Param("now") Instant now);

    /** 启动时把上次进程留下的未完成任务标为失败。 */
    @Update("""
            UPDATE task SET status = 'FAILED', fail_reason = #{reason}, finished_at = #{now}, updated_at = #{now}
            WHERE status IN ('PENDING', 'RUNNING')
            """)
    int failAllUnfinished(@Param("reason") String reason, @Param("now") Instant now);

    // ---- 读取 ----

    @Select("""
            SELECT id, user_id, template_id, status, stage, result_url, fail_reason, sim_config,
                   created_at, started_at, finished_at, updated_at
            FROM task WHERE id = #{id}
            """)
    TaskEntity findById(@Param("id") long id);

    @Select("SELECT id, user_id, result_url FROM task WHERE id = #{id} AND user_id = #{userId}")
    TaskEntity findOwnedResult(@Param("id") long id, @Param("userId") String userId);

    @Select("SELECT asset_id FROM task_asset WHERE task_id = #{taskId} ORDER BY slot_index")
    List<Long> findAssetIds(@Param("taskId") long taskId);

    @Select("SELECT COUNT(*) FROM task WHERE user_id = #{userId}")
    long countByUser(@Param("userId") String userId);

    /** XML：关联 template 取标题。 */
    TaskView findViewOwned(@Param("id") long id, @Param("userId") String userId);

    /** XML：分页，按创建时间倒序。 */
    List<TaskView> findViewsByUser(
            @Param("userId") String userId, @Param("limit") int limit, @Param("offset") long offset);
}
