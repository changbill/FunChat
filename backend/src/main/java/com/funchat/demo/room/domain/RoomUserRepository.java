package com.funchat.demo.room.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoomUserRepository extends JpaRepository<RoomUser, Long> {

    // 특정 방의 현재 참여 인원수 조회
    int countByRoomId(Long roomId);

    // 특정 유저가 특정 방에 이미 있는지 확인
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    // 특정 방의 모든 참여자 정보 조회 (Fetch Join으로 User 정보까지 한 번에)
    @Query("SELECT ru FROM RoomUser ru JOIN FETCH ru.user WHERE ru.room.id = :roomId")
    List<RoomUser> findAllByRoomIdWithUser(Long roomId);

    // 유저와 방 ID로 특정 참여 정보 조회 (퇴장/수정 시 사용)
    Optional<RoomUser> findByRoomIdAndUserId(Long roomId, Long userId);
}