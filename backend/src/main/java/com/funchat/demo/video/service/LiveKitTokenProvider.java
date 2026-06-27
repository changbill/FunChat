package com.funchat.demo.video.service;

import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
@EnableConfigurationProperties(LiveKitProperties.class)
public class LiveKitTokenProvider {

    private static final long DEFAULT_TOKEN_TTL_SECONDS = 3600;

    private final LiveKitProperties properties;

    public LiveKitTokenProvider(LiveKitProperties properties) {
        this.properties = properties;
    }

    public LiveKitIssuedToken issueJoinToken(String roomName, String identity, String participantName) {
        validateProperties();

        long ttl = properties.tokenTtlSeconds() > 0 ? properties.tokenTtlSeconds() : DEFAULT_TOKEN_TTL_SECONDS;
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttl);
        SecretKey key = Keys.hmacShaKeyFor(properties.apiSecret().getBytes(StandardCharsets.UTF_8));

        Map<String, Object> videoGrant = Map.of(
                "room", roomName,
                "roomJoin", true,
                "canPublish", true,
                "canPublishData", true,
                "canSubscribe", true
        );

        String token = Jwts.builder()
                .issuer(properties.apiKey())
                .subject(identity)
                .claim("name", participantName)
                .claim("video", videoGrant)
                .notBefore(Date.from(now))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();

        return new LiveKitIssuedToken(token, expiresAt);
    }

    public String liveKitUrl() {
        return properties.url();
    }

    private void validateProperties() {
        if (isBlank(properties.apiKey()) || isBlank(properties.apiSecret()) || isBlank(properties.url())) {
            throw new BusinessException(ErrorCode.VIDEO_CONFIG_INVALID);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record LiveKitIssuedToken(String token, Instant expiresAt) {
    }
}
