package com.funchat.demo.global.util;

import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.util.Optional;
import java.util.UUID;

@Component
public class InstanceIdProvider {
    private final String instanceId = resolveInstanceId();

    public String get() {
        return instanceId;
    }

    private static String resolveInstanceId() {
        String fromEnv = System.getenv("INSTANCE_ID");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isBlank()) {
            return hostname.trim();
        }

        // Last resort: stable-ish per process start
        String jvmName = Optional.ofNullable(ManagementFactory.getRuntimeMXBean().getName()).orElse("");
        if (!jvmName.isBlank()) {
            return "jvm-" + jvmName;
        }

        return "instance-" + UUID.randomUUID();
    }
}

