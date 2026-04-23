package com.funchat.demo.global.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatConstants {
    // Durable path (exactly-once-ish persistence via consumer group)
    public static final String STREAM_PERSIST = "chat:stream:persist";
    public static final String PERSIST_GROUP = "chat:persist:group";

    // Best-effort real-time fanout path (all instances subscribe)
    public static final String PUBSUB_FANOUT_CHANNEL = "chat:pubsub:fanout";

    public static final String ROOM_ID = "roomId";
    public static final String SENDER_ID = "senderId";
    public static final String SENDER_NICKNAME = "senderNickname";
    public static final String MESSAGE_CONTENT = "content";
    public static final String MESSAGE_TYPE = "type";
    public static final String SUBSCRIPTION_URL = "/sub/chat/";
}
