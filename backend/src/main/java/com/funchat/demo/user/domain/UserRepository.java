package com.funchat.demo.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    long countByRoomId(Long roomId);
    List<User> findByRoomId(Long roomId);
    boolean existsByIdAndRoomIsNotNull(Long userId);
    boolean existsByIdAndRoom_Id(Long userId, Long roomId);
    @Query("SELECT u FROM User u WHERE u.room.id = :roomId AND u.id <> :userId")
    Optional<User> findFirstByRoomIdAndIdNot(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
