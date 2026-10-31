package com.xiaoying.simulation;

import com.xiaoying.task.TaskStage;
import jakarta.validation.Valid;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开发用接口：查看和修改模拟配置。只在 app.dev-api.enabled=true 时注册，否则这些路径返回 404。
 *
 * <p>修改只影响之后提交的任务；已提交的任务使用提交时的配置快照。
 */
@RestController
@RequestMapping("/api/dev/simulation")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.dev-api", name = "enabled", havingValue = "true")
public class SimulationController {

    private final SimulationService simulationService;

    @GetMapping
    public SimulationVO get() {
        return SimulationVO.from(simulationService.current());
    }

    /** 整体替换配置。未给出的耗时字段沿用当前值。 */
    @PutMapping
    public SimulationVO update(@Valid @RequestBody UpdateSimulationRequest request) {
        SimulationConfig old = simulationService.current();
        Map<TaskStage, Long> delays = new EnumMap<>(TaskStage.class);
        delays.put(TaskStage.CHECK_ASSETS, orElse(request.checkAssetsMs(), old.delayOf(TaskStage.CHECK_ASSETS)));
        delays.put(TaskStage.GENERATE, orElse(request.generateMs(), old.delayOf(TaskStage.GENERATE)));
        delays.put(
                TaskStage.ORGANIZE_RESULT, orElse(request.organizeResultMs(), old.delayOf(TaskStage.ORGANIZE_RESULT)));
        TaskStage failStage =
                request.failStage() == null ? old.failStage() : SimulationService.parseFailStage(request.failStage());
        return SimulationVO.from(simulationService.replace(new SimulationConfig(delays, failStage)));
    }

    private static long orElse(Long value, long fallback) {
        return value == null ? fallback : value;
    }
}
