package com.funchat.demo.chat.service.redis;

import com.funchat.demo.chat.domain.dto.RedisStreamsMessageDto;
import com.funchat.demo.chat.service.MessageBrokerAdapter;
import com.funchat.demo.chat.service.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.funchat.demo.global.constants.ChatConstants.*;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class RedisMessageBrokerAdapter implements MessageBrokerAdapter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConnectionFactory connectionFactory;
    private final TaskExecutor streamTaskExecutor = createStreamTaskExecutor();

    private TaskExecutor createStreamTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setThreadNamePrefix("stream-listener-");
        executor.initialize();
        return executor;
    }

    /**
     * ObjectMapper.convertValue(dto)는 숫자·enum 필드를 JSON 문자열로 바꾸는 과정에서 큰따옴표가 섞인 문자열이 들어갈 수 있다.
     */
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
    public void publish(String topic, Object message) {
        RedisStreamsMessageDto dto = (RedisStreamsMessageDto) message;
        Map<String, String> fieldMap = toStreamFields(dto);
        redisTemplate.opsForStream().add(topic, fieldMap);// 레디스 스트림에 해당 레코드 발행
    }

    @Override
    public void subscribe(String topic, String consumerGroup, MessageHandler handler) {
        // 도중 redis 오류 발생 시 init() 호출 불가하기 때문에 방어적으로 사용
        try {
            redisTemplate.opsForStream().createGroup(topic, consumerGroup);
        } catch (Exception e) {
            // 존재할 시 ignore
        }

        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                .batchSize(10)
                .executor(streamTaskExecutor) // 스레드 부족 방지
                .pollTimeout(Duration.ofSeconds(2))
                .build();

        var container = StreamMessageListenerContainer.create(connectionFactory, options);
        container.receive(
                Consumer.from(consumerGroup, "my-instance"),
                StreamOffset.create(topic, ReadOffset.lastConsumed()),
                message -> {
                    handler.handle(message.getValue());
                }
        );

        container.start();
        log.info("Redis Stream 구독 시작: topic={}, group={}", topic, consumerGroup);
    }
}
