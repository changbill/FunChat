package com.funchat.demo.chat.service;

import com.funchat.demo.chat.domain.dto.MessageResponse;
import com.funchat.demo.chat.domain.dto.RedisStreamsMessageDto;
import com.funchat.demo.chat.service.redis.RedisMessageBrokerAdapter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.funchat.demo.chat.domain.ChatConstants.*;

@Service
@RequiredArgsConstructor
public class MessageBrokerChatService {

    private final RedisMessageBrokerAdapter messageBrokerAdapter;
    private final SimpMessagingTemplate messagingTemplate;
    private final MongoTemplate mongoTemplate;

    // 스프링 생명주기 상 빈 주입을 모두 마친 직후에 실행할 로직
    @PostConstruct
    public void init() {
        // messageHandler: 메시지 받은 후 실행될 콜백 함수
        messageBrokerAdapter.subscribe(STREAM_TOPIC, CONSUMER_GROUP, message -> {
            String roomId = message.get(ROOM_ID);
            messagingTemplate.convertAndSend(SUBSCRIPTION_URL + roomId, message);
            // 웹소켓으로 /sub/chat/ + roomId를 구독하고 있는 뷰단에 메시지 전송
        });
    }

    public void sendChatMessageToRedisStreams(String roomId, String inputMessage) {
        RedisStreamsMessageDto message = RedisStreamsMessageDto.builder()
                .roomId(Long.parseLong(roomId))
                .senderId(1L)   // todo: 임시 유저 정보
                .senderNickname("User")
                .message(inputMessage)
                .createdAt(LocalDateTime.now())
                .build();

        // 모든 채팅방 공통 토픽, roomId로 채팅방 구분
        messageBrokerAdapter.publish(STREAM_TOPIC, message);
    }

    public List<MessageResponse> getPreviousMessage(String roomId, String cursorId, int size) {
        List<MessageResponse> combinedResults = new ArrayList<>();
        // 1. Redis Stream에서 cursorId보다 작은(이전) 데이터 조회
        List<MapRecord<String, String, String>> redisRecords =
                messageBrokerAdapter.fetchMessagesBefore(STREAM_TOPIC, cursorId, size);

        for(var record : redisRecords) {
            combinedResults.add(convertToDto(record.getId().getValue(), record.getValue()));
        }

        // 2. Redis에 데이터가 부족하다면 MongoDB 조회
        if (combinedResults.size() < size) {
            int remaining = size - combinedResults.size();

            String lastIdInRedis = combinedResults.isEmpty() ? cursorId : Long.toString(combinedResults.get(combinedResults.size() - 1).getMessageId());

            // MongoDB 쿼리: _id < cursorId 인 데이터를 생성일 역순으로 조회
            Query query = new Query(
                    Criteria.where(ROOM_ID).is(roomId)
                            .and("_id").lt(lastIdInRedis) // Cursor보다 작은 ID (과거)
            ).with(Sort.by(Sort.Direction.DESC, "_id")).limit(remaining);

            List<MessageResponse> mongoMessages = mongoTemplate.find(query, MessageResponse.class, MESSAGE);
            combinedResults.addAll(mongoMessages);
        }

        return combinedResults;
    }

    private MessageResponse convertToDto(String id, Map<String, String> map) {
        return MessageResponse.builder()
                .messageId(Long.parseLong(id))
                .roomId(Long.parseLong(map.get(ROOM_ID)))
                .senderId(Long.parseLong(map.get(SENDER_ID)))
                .senderNickname(map.get(SENDER_NICKNAME))
                .message(map.get(MESSAGE))
                .createdAt(LocalDateTime.parse(map.get(CREATED_AT)))
                .build();
    }
}
