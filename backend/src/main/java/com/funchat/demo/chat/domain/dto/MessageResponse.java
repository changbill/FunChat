package com.funchat.demo.chat.domain.dto;

import com.funchat.demo.chat.domain.ChatMessage;
import lombok.Builder;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Builder
public record MessageResponse(
        String messageId,
        long roomId,
        long senderId,
        String senderNickname,
        String content,
        LocalDateTime createdAt
) {
    public static MessageResponse from(ChatMessage chatMessage) {
        return MessageResponse.builder()
                .messageId(chatMessage.getId())
                .roomId(chatMessage.getRoomId())
                .senderId(chatMessage.getSenderId())
                .senderNickname(chatMessage.getSenderNickname())
                .content(chatMessage.getContent())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }
}