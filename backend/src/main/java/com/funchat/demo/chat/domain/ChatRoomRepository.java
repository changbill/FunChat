package com.funchat.demo.chat.domain;

import com.funchat.demo.room.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT r FROM ChatRoom r LEFT JOIN FETCH r.chatRoomUsers")
    List<ChatRoom> findAllWithUsers();
}
