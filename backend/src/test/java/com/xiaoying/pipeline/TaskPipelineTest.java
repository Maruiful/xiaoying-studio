package com.xiaoying.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.xiaoying.simulation.SimulationConfig;
import com.xiaoying.simulation.SimulationService;
import com.xiaoying.task.TaskEntity;
import com.xiaoying.task.TaskMapper;
import com.xiaoying.task.TaskStage;
import com.xiaoying.task.TaskStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** 用假的步骤和 mock 的状态记录器验证流水线的编排：顺序、失败中断、状态更新失败时停止。 */
class TaskPipelineTest {

    private static final long TASK_ID = 7L;

    private final TaskStateRecorder recorder = mock(TaskStateRecorder.class);
    private final TaskMapper taskMapper = mock(TaskMapper.class);
    private final SimulationService simulationService = mock(SimulationService.class);
    private final List<TaskStage> executed = new ArrayList<>();

    @BeforeEach
    void setUp() {
        TaskEntity task = new TaskEntity();
        task.setId(TASK_ID);
        task.setStatus(TaskStatus.PENDING);
        task.setSimConfig("{}");
        given(taskMapper.findById(TASK_ID)).willReturn(task);
        given(taskMapper.findAssetIds(TASK_ID)).willReturn(List.of(1L, 2L));
        given(simulationService.fromSnapshot("{}")).willReturn(new SimulationConfig(Map.of(), null));
        given(recorder.stepStarted(eq(TASK_ID), org.mockito.ArgumentMatchers.any()))
                .willReturn(true);
        given(recorder.stepSucceeded(eq(TASK_ID), org.mockito.ArgumentMatchers.any(), anyString()))
                .willReturn(true);
        given(recorder.taskSucceeded(eq(TASK_ID), anyString())).willReturn(true);
    }

    @Test
    @DisplayName("按 素材检查 → 模拟生成 → 结果整理 的顺序执行，与注入顺序无关")
    void runsStepsInStageOrder() {
        // 故意打乱注入顺序
        TaskPipeline pipeline = pipeline(
                step(TaskStage.ORGANIZE_RESULT, null),
                step(TaskStage.CHECK_ASSETS, null),
                step(TaskStage.GENERATE, null));

        pipeline.run(TASK_ID);

        assertThat(executed).containsExactly(TaskStage.CHECK_ASSETS, TaskStage.GENERATE, TaskStage.ORGANIZE_RESULT);
        var order = inOrder(recorder);
        order.verify(recorder).stepStarted(TASK_ID, TaskStage.CHECK_ASSETS);
        order.verify(recorder).stepSucceeded(TASK_ID, TaskStage.CHECK_ASSETS, "CHECK_ASSETS done");
        order.verify(recorder).stepStarted(TASK_ID, TaskStage.GENERATE);
        order.verify(recorder).stepSucceeded(TASK_ID, TaskStage.GENERATE, "GENERATE done");
        order.verify(recorder).stepStarted(TASK_ID, TaskStage.ORGANIZE_RESULT);
        order.verify(recorder).taskSucceeded(TASK_ID, "ORGANIZE_RESULT done");
        verify(recorder, never()).stepFailed(anyLong(), org.mockito.ArgumentMatchers.any(), anyString());
    }

    @ParameterizedTest(name = "{0} 失败")
    @EnumSource(TaskStage.class)
    @DisplayName("某一步失败后记录失败阶段和原因，后续步骤不执行")
    void stopsAtFailedStage(TaskStage failing) {
        TaskPipeline pipeline = pipeline(
                step(TaskStage.CHECK_ASSETS, failing),
                step(TaskStage.GENERATE, failing),
                step(TaskStage.ORGANIZE_RESULT, failing));

        pipeline.run(TASK_ID);

        List<TaskStage> expected = List.of(TaskStage.values()).subList(0, failing.ordinal() + 1);
        assertThat(executed).containsExactlyElementsOf(expected);
        verify(recorder).stepFailed(TASK_ID, failing, failing + " 模拟失败");
        verify(recorder, never()).taskSucceeded(anyLong(), anyString());
        for (TaskStage later : TaskStage.values()) {
            if (later.ordinal() > failing.ordinal()) {
                verify(recorder, never()).stepStarted(TASK_ID, later);
            }
        }
    }

    @Test
    @DisplayName("步骤抛出未预期的异常时，按内部错误记录失败，不把异常信息暴露给用户")
    void unexpectedExceptionBecomesInternalError() {
        TaskStep broken = new TaskStep() {
            @Override
            public TaskStage stage() {
                return TaskStage.GENERATE;
            }

            @Override
            public String execute(StepContext context) {
                executed.add(stage());
                throw new IllegalStateException("NullPointer at line 42");
            }
        };
        TaskPipeline pipeline =
                pipeline(step(TaskStage.CHECK_ASSETS, null), broken, step(TaskStage.ORGANIZE_RESULT, null));

        pipeline.run(TASK_ID);

        assertThat(executed).containsExactly(TaskStage.CHECK_ASSETS, TaskStage.GENERATE);
        verify(recorder).stepFailed(TASK_ID, TaskStage.GENERATE, TaskPipeline.INTERNAL_ERROR_REASON);
    }

    @Test
    @DisplayName("进入某一步时状态更新失败（任务已不可执行），立即停止，不执行该步")
    void stopsWhenStateUpdateRejected() {
        given(recorder.stepStarted(TASK_ID, TaskStage.GENERATE)).willReturn(false);
        TaskPipeline pipeline = pipeline(
                step(TaskStage.CHECK_ASSETS, null),
                step(TaskStage.GENERATE, null),
                step(TaskStage.ORGANIZE_RESULT, null));

        pipeline.run(TASK_ID);

        assertThat(executed).containsExactly(TaskStage.CHECK_ASSETS);
        verify(recorder, never()).stepFailed(anyLong(), org.mockito.ArgumentMatchers.any(), anyString());
        verify(recorder, never()).taskSucceeded(anyLong(), anyString());
    }

    @Test
    @DisplayName("任务不是 PENDING（例如已被标为失败）时不执行任何步骤")
    void skipsTaskThatIsNotPending() {
        TaskEntity done = new TaskEntity();
        done.setId(TASK_ID);
        done.setStatus(TaskStatus.FAILED);
        given(taskMapper.findById(TASK_ID)).willReturn(done);
        TaskPipeline pipeline = pipeline(
                step(TaskStage.CHECK_ASSETS, null),
                step(TaskStage.GENERATE, null),
                step(TaskStage.ORGANIZE_RESULT, null));

        pipeline.run(TASK_ID);

        assertThat(executed).isEmpty();
        verifyNoMoreInteractions(recorder);
    }

    @Test
    @DisplayName("缺少某个阶段或重复注册时启动失败")
    void rejectsIncompleteStepSet() {
        assertThatThrownBy(() -> pipeline(step(TaskStage.CHECK_ASSETS, null), step(TaskStage.GENERATE, null)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> pipeline(
                        step(TaskStage.CHECK_ASSETS, null),
                        step(TaskStage.CHECK_ASSETS, null),
                        step(TaskStage.GENERATE, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    private TaskPipeline pipeline(TaskStep... steps) {
        return new TaskPipeline(List.of(steps), recorder, taskMapper, simulationService);
    }

    /** 记录执行顺序的假步骤；stage == failing 时抛出 StepFailedException。 */
    private TaskStep step(TaskStage stage, TaskStage failing) {
        return new TaskStep() {
            @Override
            public TaskStage stage() {
                return stage;
            }

            @Override
            public String execute(StepContext context) {
                assertThat(context.assetIds()).containsExactly(1L, 2L);
                executed.add(stage);
                if (stage == failing) {
                    throw new StepFailedException(stage + " 模拟失败");
                }
                return stage + " done";
            }
        };
    }
}
