package com.funchat.demo.chat.service.redis;

import com.funchat.demo.chat.domain.dto.RedisStreamsMessageDto;
import com.funchat.demo.chat.service.ChatPersistBroker;
import com.funchat.demo.global.util.InstanceIdProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.funchat.demo.global.constants.ChatConstants.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStreamsPersistBroker implements ChatPersistBroker {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConnectionFactory connectionFactory;
    private final InstanceIdProvider instanceIdProvider;

    private final ThreadPoolTaskExecutor executor = createExecutor();

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;

    private ThreadPoolTaskExecutor createExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setThreadNamePrefix("persist-stream-");
        ex.initialize();
        return ex;
    }

    private static Map<String, String> toStreamFields(RedisStreamsMessageDto dto) {
        Map<String, String> fieldMap = new LinkedHashMap<>();
        fieldMap.put(ROOM_ID, String.valueOf(dto.roomId()));
        fieldMap.put(SENDER_ID, String.valueOf(dto.senderId()));
        fieldMap.put(SENDER_NICKNAME, dto.senderNickname());
        fieldMap.put(MESSAGE_CONTENT, dto.content());
        fieldMap.put(MESSAGE_TYPE, dto.type().name());
        fieldMap.put("createdAt", dto.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return fieldMap;
    }

    @Override
    public void publish(RedisStreamsMessageDto message) {
        redisTemplate.opsForStream().add(STREAM_PERSIST, toStreamFields(message));
    }

    @Override
    public synchronized void subscribe(Consumer<RedisStreamsMessageDto> handler) {
        if (container != null) {
            return;
        }

        // Create group defensively (ignore if exists). Stream might not exist yet.
        try {
            redisTemplate.opsForStream().createGroup(STREAM_PERSIST, ReadOffset.latest(), PERSIST_GROUP);
        } catch (Exception ignored) {
            // exists or stream missing; will be created on first add
        }

        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                .batchSize(10)
                .executor(executor)
                .pollTimeout(Duration.ofSeconds(2))
                .build();

        container = StreamMessageListenerContainer.create(connectionFactory, options);

        String instanceId = instanceIdProvider.get();
        container.receive(
                org.springframework.data.redis.connection.stream.Consumer.from(PERSIST_GROUP, instanceId),
                StreamOffset.create(STREAM_PERSIST, ReadOffset.lastConsumed()),
                record -> {
                    try {
                        String createdAtRaw = record.getValue().get("createdAt");
                        LocalDateTime createdAt = createdAtRaw == null ? LocalDateTime.now() : LocalDateTime.parse(createdAtRaw, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                        RedisStreamsMessageDto dto = RedisStreamsMessageDto.builder()
                                .roomId(Long.parseLong(record.getValue().getOrDefault(ROOM_ID, "0")))
                                .senderId(Long.parseLong(record.getValue().getOrDefault(SENDER_ID, "0")))
                                .senderNickname(record.getValue().get(SENDER_NICKNAME))
                                .content(record.getValue().get(MESSAGE_CONTENT))
                                .type(com.funchat.demo.chat.domain.MessageType.valueOf(record.getValue().getOrDefault(MESSAGE_TYPE, "TEXT")))
                                .createdAt(createdAt)
                                .build();

                        handler.accept(dto);

                        // ACK after successful handling
                        redisTemplate.opsForStream().acknowledge(STREAM_PERSIST, PERSIST_GROUP, record.getId());
                    } catch (Exception e) {
                        log.error("Persist stream handling failed. id={}, stream={}", record.getId(), STREAM_PERSIST, e);
                        // Leave pending for retry/claim policy.
                    }
                }
        );

        container.start();
        log.info("Redis Streams persist subscription started. stream={}, group={}, consumer={}", STREAM_PERSIST, PERSIST_GROUP, instanceId);
    }
}

