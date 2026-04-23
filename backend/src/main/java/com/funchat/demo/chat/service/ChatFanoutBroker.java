package com.funchat.demo.chat.service;

import com.funchat.demo.chat.domain.dto.RedisStreamsMessageDto;

import java.util.function.Consumer;

/**
 * Best-effort real-time fanout (e.g., Redis Pub/Sub).
 */
public interface ChatFanoutBroker {
    void publish(RedisStreamsMessageDto message);

    void subscribe(Consumer<RedisStreamsMessageDto> handler);
}

