package com.funchat.demo.chat.service.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate redisTemplate;

    public void saveUserCurrentRoom(Long userId, Long roomId) {
        redisTemplate.opsForValue().set(
                "user:pos:" + userId,
                roomId,
                Duration.ofHours(2)
        );
    }

    public void deleteUserCurrentRoom(Long userId) {
        redisTemplate.delete("user:pos:"+ userId);
    }
}
