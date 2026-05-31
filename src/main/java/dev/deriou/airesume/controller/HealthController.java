package dev.deriou.airesume.controller;

import dev.deriou.airesume.common.api.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public HealthController(JdbcTemplate jdbcTemplate, StringRedisTemplate stringRedisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Integer mysql = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        String redis = stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.ping());

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", "ai-resume");
        status.put("mysql", mysql != null && mysql == 1 ? "UP" : "DOWN");
        status.put("redis", "PONG".equalsIgnoreCase(redis) ? "UP" : "DOWN");
        return ApiResponse.success(status);
    }
}
