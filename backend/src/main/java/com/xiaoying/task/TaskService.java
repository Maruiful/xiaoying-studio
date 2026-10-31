package com.xiaoying.task;

import com.xiaoying.asset.AssetEntity;
import com.xiaoying.asset.AssetService;
import com.xiaoying.asset.FileStorage;
import com.xiaoying.common.BizException;
import com.xiaoying.common.ErrorCode;
import com.xiaoying.common.PageVO;
import com.xiaoying.pipeline.OrganizeResultStep;
import com.xiaoying.simulation.SimulationService;
import com.xiaoying.template.TemplateEntity;
import com.xiaoying.template.TemplateService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    static final int MAX_PAGE_SIZE = 50;

    private final TemplateService templateService;
    private final AssetService assetService;
    private final SimulationService simulationService;
    private final TaskMapper taskMapper;
    private final TaskDispatcher dispatcher;
    private final FileStorage fileStorage;
    private final Clock clock;

    /**
     * 校验参数并创建 PENDING 任务，事务提交后派发到线程池，立即返回任务 ID，不等待执行。
     *
     * <p>事务里只有校验查询和两条 INSERT。
     */
    @Transactional
    public CreateTaskVO create(String userId, CreateTaskRequest request) {
        TemplateEntity template = templateService.requireTemplate(request.templateId());
        validateAssets(userId, template, request.assetIds());

        TaskEntity task = new TaskEntity();
        task.setUserId(userId);
        task.setTemplateId(template.getId());
        task.setSimConfig(simulationService.toSnapshot(simulationService.current()));
        task.setCreatedAt(Instant.now(clock).truncatedTo(ChronoUnit.MILLIS));
        taskMapper.insert(task);
        taskMapper.insertTaskAssets(task.getId(), request.assetIds());

        dispatcher.dispatchAfterCommit(task.getId());
        return new CreateTaskVO(task.getId());
    }

    /** 只读，不会推动任务进入下一步。 */
    public TaskVO get(String userId, long taskId) {
        TaskView view = taskMapper.findViewOwned(taskId, userId);
        if (view == null) {
            throw new BizException(ErrorCode.TASK_NOT_FOUND);
        }
        return TaskVO.from(view);
    }

    /** 当前用户的历史任务，按创建时间倒序。page 从 1 开始。 */
    public PageVO<TaskVO> list(String userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        long offset = (long) (safePage - 1) * safeSize;
        List<TaskVO> items = taskMapper.findViewsByUser(userId, safeSize, offset).stream()
                .map(TaskVO::from)
                .toList();
        return new PageVO<>(items, taskMapper.countByUser(userId), safePage, safeSize);
    }

    /** 读取结果图。任务不存在、不属于当前用户、还没有结果时都返回 404。 */
    public Resource loadResult(String userId, long taskId) {
        TaskEntity task = taskMapper.findOwnedResult(taskId, userId);
        if (task == null || task.getResultUrl() == null) {
            throw new BizException(ErrorCode.RESULT_NOT_FOUND);
        }
        String path = OrganizeResultStep.resultPath(taskId);
        if (!fileStorage.exists(path)) {
            throw new BizException(ErrorCode.RESULT_NOT_FOUND);
        }
        return fileStorage.load(path);
    }

    private void validateAssets(String userId, TemplateEntity template, List<Long> assetIds) {
        if (assetIds.size() != template.getRequiredImageCount()) {
            throw new BizException(
                    ErrorCode.TASK_ASSET_COUNT_MISMATCH,
                    "该模板需要 " + template.getRequiredImageCount() + " 张图片",
                    Map.of("required", template.getRequiredImageCount(), "actual", assetIds.size()));
        }
        if (new HashSet<>(assetIds).size() != assetIds.size()) {
            throw new BizException(ErrorCode.TASK_ASSET_INVALID, "同一张图片不能重复使用");
        }
        Map<Long, AssetEntity> owned = assetService.findOwned(userId, assetIds).stream()
                .collect(Collectors.toMap(AssetEntity::getId, Function.identity()));
        for (Long id : assetIds) {
            AssetEntity asset = owned.get(id);
            // 不存在和属于他人返回相同的错误，不暴露素材是否存在
            if (asset == null) {
                throw new BizException(ErrorCode.TASK_ASSET_INVALID, "素材不存在", Map.of("assetId", id));
            }
            if (!asset.getTemplateId().equals(template.getId())) {
                throw new BizException(ErrorCode.TASK_ASSET_INVALID, "素材不属于该模板", Map.of("assetId", id));
            }
        }
    }
}
