package com.funchat.demo.chat.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {
    // 특정 Redis ID보다 작은(과거) 데이터를 찾는 쿼리 사용
    List<Message> findByRoomIdAndIdLessThan(Long roomId, String lastId, Pageable pageable);

    List<Message> findByRoomId(Long roomId, Pageable pageable);
}
