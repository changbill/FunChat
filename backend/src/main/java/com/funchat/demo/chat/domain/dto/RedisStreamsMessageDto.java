package com.funchat.demo.chat.domain.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RedisStreamsMessageDto(
        long roomId,
        long senderId,
        String senderNickname,
        String message,
        LocalDateTime createdAt
) {
}
