package com.funchat.demo.chat.domain;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<Message, String> {
    List<Message> findByRoomIdOrderByCreatedAtAsc(Long roomId);
}
