package com.funchat.demo.video.domain;

import com.funchat.demo.global.domain.BaseTimeEntity;
import com.funchat.demo.room.domain.Room;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class VideoSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Room room;

    @Column(nullable = false)
    private String livekitRoomName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoSessionStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private VideoSession(Room room, String livekitRoomName) {
        this.room = room;
        this.livekitRoomName = livekitRoomName;
        this.status = VideoSessionStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
    }

    public static VideoSession start(Room room, String livekitRoomName) {
        return new VideoSession(room, livekitRoomName);
    }

    public void end() {
        this.status = VideoSessionStatus.ENDED;
        this.endedAt = LocalDateTime.now();
    }
}
