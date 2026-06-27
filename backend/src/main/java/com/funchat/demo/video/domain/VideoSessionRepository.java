package com.funchat.demo.video.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoSessionRepository extends JpaRepository<VideoSession, Long> {
    Optional<VideoSession> findFirstByRoomIdAndStatusOrderByIdDesc(Long roomId, VideoSessionStatus status);
}
