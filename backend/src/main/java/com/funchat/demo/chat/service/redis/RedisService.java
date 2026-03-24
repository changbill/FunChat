package com.funchat.demo.chat.service.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    private String REFRESH_TOKEN_PREFIX = "RT:";
    private String USER_CURRENT_ROOM_PREFIX = "user:pos:";
    private String BLACKLIST_PREFIX = "blacklist:";

    public String getRefreshToken(String email) {
        return (String) redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + email);
    }

    public void saveRefreshToken(String email, String refreshToken, Duration duration) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + email,
                refreshToken,
                duration
        );
    }

    public void deleteRefreshToken(String email) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + email);
    }

    public Long getUserCurrentRoomId(Long userId) {
        return (Long) redisTemplate.opsForValue().get(USER_CURRENT_ROOM_PREFIX + userId);
    }

    public void saveUserCurrentRoomId(Long userId, Long roomId) {
        redisTemplate.opsForValue().set(
                USER_CURRENT_ROOM_PREFIX + userId,
                roomId,
                Duration.ofHours(2)
        );
    }

    public void deleteUserCurrentRoomId(Long userId) {
        redisTemplate.delete(USER_CURRENT_ROOM_PREFIX+ userId);
    }

    public void saveBlacklist(String accessToken, String logout, Duration duration) {
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + accessToken,
                logout,
                duration
        );
    }

    public boolean isBlacklisted(String accessToken) {
        return redisTemplate.hasKey(BLACKLIST_PREFIX + accessToken);
    }
}
