package com.funchat.demo.chat.service.redis;

import com.funchat.demo.chat.domain.dto.RedisStreamsMessageDto;
import com.funchat.demo.chat.service.ChatFanoutBroker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static com.funchat.demo.global.constants.ChatConstants.PUBSUB_FANOUT_CHANNEL;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisPubSubFanoutBroker implements ChatFanoutBroker {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConnectionFactory connectionFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private RedisMessageListenerContainer container;
    private volatile Consumer<RedisStreamsMessageDto> handler;

    @PostConstruct
    public void start() {
        container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(new InternalListener(), new ChannelTopic(PUBSUB_FANOUT_CHANNEL));
        container.afterPropertiesSet();
        container.start();
        log.info("Redis Pub/Sub fanout subscribed. channel={}", PUBSUB_FANOUT_CHANNEL);
    }

    @PreDestroy
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }

    @Override
    public void publish(RedisStreamsMessageDto message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(PUBSUB_FANOUT_CHANNEL, payload);
        } catch (Exception e) {
            log.error("Failed to publish fanout message.", e);
        }
    }

    @Override
    public void subscribe(Consumer<RedisStreamsMessageDto> handler) {
        this.handler = handler;
    }

    private class InternalListener implements MessageListener {
        @Override
        public void onMessage(Message message, byte[] pattern) {
            Consumer<RedisStreamsMessageDto> h = handler;
            if (h == null) {
                return;
            }

            try {
                String payload = new String(message.getBody(), StandardCharsets.UTF_8);
                RedisStreamsMessageDto dto = objectMapper.readValue(payload, RedisStreamsMessageDto.class);
                h.accept(dto);
            } catch (Exception e) {
                log.error("Failed to handle fanout message.", e);
            }
        }
    }
}

