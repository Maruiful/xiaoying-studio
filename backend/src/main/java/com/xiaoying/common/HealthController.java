package com.xiaoying.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 健康检查：同时验证 MyBatis 到 MySQL 的连接。供 Compose 健康检查和前端占位页使用。 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final HealthMapper healthMapper;

    @GetMapping("/api/health")
    public ResponseEntity<HealthVO> health() {
        try {
            healthMapper.ping();
            return ResponseEntity.ok(new HealthVO("UP", "UP"));
        } catch (DataAccessException e) {
            log.warn("数据库健康检查失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new HealthVO("DOWN", "DOWN"));
        }
    }

    public record HealthVO(String status, String database) {}
}
