package com.funchat.demo.chat.service;

import com.funchat.demo.chat.domain.dto.RedisStreamsMessageDto;

import java.util.function.Consumer;

/**
 * Durable chat message pipeline (e.g., Redis Streams + consumer group).
 */
public interface ChatPersistBroker {
    void publish(RedisStreamsMessageDto message);

    /**
     * Subscribe and process messages durably.
     * Implementation is responsible for ack/retry semantics.
     */
    void subscribe(Consumer<RedisStreamsMessageDto> handler);
}

