package com.funchat.demo.global.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatConstants {
    public static final String STREAM_TOPIC = "chat-stream";
    public static final String CONSUMER_GROUP = "chat-group";
    public static final String ROOM_ID = "roomId";
    public static final String SENDER_ID = "senderId";
    public static final String SENDER_NICKNAME = "senderNickname";
    public static final String MESSAGE_CONTENT = "content";
    public static final String MESSAGE_TYPE = "type";
    public static final String SUBSCRIPTION_URL = "/sub/chat/";
}
