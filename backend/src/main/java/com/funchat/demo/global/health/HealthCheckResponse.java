package com.funchat.demo.global.health;

import java.util.Map;

public record HealthCheckResponse(String status, Map<String, String> checks) {

    public boolean healthy() {
        return "UP".equals(status);
    }
}
