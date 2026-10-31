package com.xiaoying.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 模拟生成器的启动默认值，对应 app.simulation.*。运行时可通过开发接口修改。
 *
 * @param failStage CHECK_ASSETS / GENERATE / ORGANIZE_RESULT / NONE
 */
@ConfigurationProperties("app.simulation")
public record SimulationProperties(
        @DefaultValue("1000") long checkAssetsMs,
        @DefaultValue("3000") long generateMs,
        @DefaultValue("1000") long organizeResultMs,
        @DefaultValue("NONE") String failStage) {}
