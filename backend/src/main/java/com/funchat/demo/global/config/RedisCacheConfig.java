package com.funchat.demo.global.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public LettuceConnectionFactory connectionFactory() {
        return new LettuceConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 1. JSON 변환을 위한 ObjectMapper 설정
        ObjectMapper mapper = new JsonMapper();

        // 2. Serializer 생성
        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(mapper);

        // 3. Key와 Value에 Serializer 적용
        template.setKeySerializer(new StringRedisSerializer()); // 키는 평범한 문자열로
        template.setValueSerializer(jsonSerializer);            // 값(객체)은 JSON으로

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);

        return template;
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(defaultConfiguration())
                .build();
    }

    private RedisCacheConfiguration defaultConfiguration() {
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        ObjectMapper objectMapper = new ObjectMapper();
        GenericJacksonJsonRedisSerializer genericJacksonJsonRedisSerializer = new GenericJacksonJsonRedisSerializer(objectMapper);
        Duration duration = Duration.ofMinutes(10);

        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringRedisSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(genericJacksonJsonRedisSerializer))
                .entryTtl(duration);
    }
}
