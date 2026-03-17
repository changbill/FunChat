package com.funchat.demo.chat.domain.dto;

import com.funchat.demo.chat.domain.Message;
import lombok.Builder;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Builder
public record MessageResponse(
        @Id
        String messageId,
        long roomId,
        long senderId,
        String senderNickname,
        String content,
        LocalDateTime createdAt
) {
    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
                .messageId(message.getId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .senderNickname(message.getSenderNickname())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}