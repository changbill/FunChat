package com.funchat.demo.chat.domain;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;

@Document(collection = "chat_message")
@CompoundIndex(name = "idx_chat_message_room_id_id_desc", def = "{'roomId': 1, '_id': -1}")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ChatMessage implements Serializable {
    @Id
    private String id; // MongoDB의 _id (ObjectId)

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
