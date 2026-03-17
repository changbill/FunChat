package com.funchat.demo.room.domain;

import com.funchat.demo.global.domain.BaseTimeEntity;
import com.funchat.demo.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Room extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private Integer maxMembers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_manager_id")
    private User manager;

    @Singular
    @OneToMany(mappedBy = "room")
    private List<RoomUser> roomUsers;

    public void update(String title, Integer maxMembers) {
        this.title = title;
        this.maxMembers = maxMembers;
    }

}
