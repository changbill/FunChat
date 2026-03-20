package com.funchat.demo.chat.domain.dto;

import com.funchat.demo.chat.domain.MessageType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RedisStreamsMessageDto(
        long roomId,
        long senderId,
        String senderNickname,
        String content,
        MessageType type,
        LocalDateTime createdAt
) {
}
