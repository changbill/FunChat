package com.funchat.demo.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    long countByRoomId(Long roomId);
    List<User> findByRoomId(Long roomId);

    // 유저가 현재 어느 방에 있는지 확인 (이미 있다면 에러 처리용)
    boolean existsByIdAndRoomIsNotNull(Long userId);
}
