package com.funchat.demo;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public abstract class TestContainerTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:latest"))
            .withExposedPorts(6379);

    @Container
    static final GenericContainer<?> mongoDb = new GenericContainer<>(DockerImageName.parse("mongo:latest"))
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        registry.add("spring.mongodb.host", mongoDb::getHost);
        registry.add("spring.mongodb.port", mongoDb::getFirstMappedPort);
    }

    @Test
    void test() {
        assertThat(redis.isRunning()).isTrue();
        assertThat(mongoDb.isRunning()).isTrue();
    }
}
