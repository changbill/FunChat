package com.funchat.demo.chat.service.redis;

import com.funchat.demo.chat.service.MessageBrokerAdapter;
import com.funchat.demo.chat.service.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.funchat.demo.global.constants.ChatConstants.ROOM_ID;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class RedisMessageBrokerAdapter implements MessageBrokerAdapter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String topic, Object message) {
        /**
         * 자바는 컴파일이 끝나면 제네릭 정보(<String, String>)를 지워버려 맵핑할 객체를 알수없어 Object로 변환한다.
         * TypeReference는 타입을 저장하는 클래스. 해당 타입 클래스로 변환한다.
         */
        TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};

        Map<String, String> fieldMap = objectMapper.convertValue(message, typeRef);

        ObjectRecord<String, Map<String, String>> record = StreamRecords.newRecord()
                .in(topic)
                .ofObject(fieldMap);

        redisTemplate.opsForStream().add(record);// 레디스 스트림에 해당 레코드 발행
    }

    @Override
    public void subscribe(String topic, String consumerGroup, MessageHandler handler) {
        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                .batchSize(10)
                .executor(streamTaskExecutor()) // 스레드 부족 방지
                .pollTimeout(Duration.ofSeconds(2))
                .build();

        var container = StreamMessageListenerContainer.create(connectionFactory, options);
        container.receive(StreamOffset.latest(topic), message -> {
            handler.handle(message.getValue());
        });

        container.start();
    }

    public TaskExecutor streamTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 기본 스레드 수
        executor.setMaxPoolSize(10); // 최대 스레드 수
        executor.setThreadNamePrefix("stream-listener-");
        executor.initialize();
        return executor;
    }
}
