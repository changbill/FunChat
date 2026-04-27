package com.funchat.demo.global.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthService {

    private static final String UP = "UP";
    private static final String DOWN = "DOWN";

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final MongoTemplate mongoTemplate;

    public HealthCheckResponse check() {
        Map<String, String> checks = new LinkedHashMap<>();
        checks.put("database", checkDatabase());
        checks.put("redis", checkRedis());
        checks.put("mongodb", checkMongo());

        boolean healthy = checks.values().stream().allMatch(UP::equals);
        return new HealthCheckResponse(healthy ? UP : DOWN, checks);
    }

    private String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(1) ? UP : DOWN;
        } catch (Exception e) {
            log.warn("Database health check failed: {}", e.getMessage());
            return DOWN;
        }
    }

    private String checkRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String response = connection.ping();
            return "PONG".equalsIgnoreCase(response) ? UP : DOWN;
        } catch (Exception e) {
            log.warn("Redis health check failed: {}", e.getMessage());
            return DOWN;
        }
    }

    private String checkMongo() {
        try {
            mongoTemplate.executeCommand(new Document("ping", 1));
            return UP;
        } catch (Exception e) {
            log.warn("MongoDB health check failed: {}", e.getMessage());
            return DOWN;
        }
    }
}
