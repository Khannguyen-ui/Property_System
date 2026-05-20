package com.homeverse.recommendation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/recommend/system")
@RequiredArgsConstructor
public class SystemHealthController {

    private final StringRedisTemplate redisTemplate;

    private final DataSource dataSource;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("redis", checkRedis());
        result.put("postgresql", checkPostgresql());
        result.put("service", "UP");

        return result;
    }

    private String checkRedis() {
        try {
            String response = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();

            return "PONG".equalsIgnoreCase(response) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String checkPostgresql() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}