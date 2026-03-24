package com.funchat.demo.chat.domain;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;

@Document(collection = "chat_message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ChatMessage implements Serializable {
    @Id
    private String id; // MongoDB의 _id (ObjectId)

    @Indexed // 쿼리 성능을 위해 인덱스 추가
    private Long roomId;

    private Long senderId;
    private String senderNickname;
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType type; // TEXT, IMAGE, JOIN, LEAVE 등

    private LocalDateTime createdAt;

    private ChatMessage(Long roomId, Long senderId, String senderNickname, String content, MessageType type) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.senderNickname = senderNickname;
        this.content = content;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    public static ChatMessage createMessage(Long roomId, Long senderId, String senderNickname, String content, MessageType type) {
        return new ChatMessage(roomId, senderId, senderNickname, content, type);
    }
}
