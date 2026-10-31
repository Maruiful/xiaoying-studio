package com.xiaoying.simulation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoying.config.SimulationProperties;
import com.xiaoying.task.TaskStage;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

/**
 * 保存当前生效的模拟配置。启动时取 app.simulation.* 的值，之后可通过开发接口修改。
 *
 * <p>配置只在内存中，重启后恢复为启动配置。
 */
@Service
public class SimulationService {

    private final ObjectMapper objectMapper;
    private final AtomicReference<SimulationConfig> current;

    public SimulationService(SimulationProperties props, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        Map<TaskStage, Long> delays = new EnumMap<>(TaskStage.class);
        delays.put(TaskStage.CHECK_ASSETS, props.checkAssetsMs());
        delays.put(TaskStage.GENERATE, props.generateMs());
        delays.put(TaskStage.ORGANIZE_RESULT, props.organizeResultMs());
        this.current = new AtomicReference<>(new SimulationConfig(delays, parseFailStage(props.failStage())));
    }

    public SimulationConfig current() {
        return current.get();
    }

    public SimulationConfig replace(SimulationConfig config) {
        current.set(config);
        return config;
    }

    /** 序列化为任务快照，写入 task.sim_config。 */
    public String toSnapshot(SimulationConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("模拟配置无法序列化", e);
        }
    }

    public SimulationConfig fromSnapshot(String json) {
        try {
            return objectMapper.readValue(json, SimulationConfig.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("任务的模拟配置快照格式错误", e);
        }
    }

    /** NONE、空值表示不失败。 */
    static TaskStage parseFailStage(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("NONE")) {
            return null;
        }
        return TaskStage.valueOf(value.strip().toUpperCase(Locale.ROOT));
    }
}
