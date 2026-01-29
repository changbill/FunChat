package com.funchat.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageDto {
    public enum MessageType {
        ENTER, TALK
    }

    private MessageType type;    // 메시지 타입
    private String roomId;      // 방 번호
    private String sender;      // 보낸 사람
    private String message;     // 메시지 내용
}