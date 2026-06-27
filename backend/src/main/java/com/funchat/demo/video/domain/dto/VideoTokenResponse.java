package com.funchat.demo.video.domain.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record VideoTokenResponse(
        Long sessionId,
        Long roomId,
        String livekitUrl,
        String livekitRoomName,
        String identity,
        String participantName,
        String token,
        LocalDateTime expiresAt
) {
}
