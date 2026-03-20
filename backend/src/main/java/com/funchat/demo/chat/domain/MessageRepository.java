package com.funchat.demo.chat.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<ChatMessage, String> {
    Slice<ChatMessage> findByRoomId(Long roomId, Pageable pageable);

    Slice<ChatMessage> findByRoomIdAndIdLessThan(Long roomId, String id, Pageable pageable);
}
