package com.funchat.demo.chat.domain.dto;

import com.funchat.demo.chat.domain.MessageType;

public record MessageRequest(
        Long roomId,
        String content,
        MessageType type
) {
}
