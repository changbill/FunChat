package com.funchat.demo.room.domain;

import com.funchat.demo.global.domain.BaseTimeEntity;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Room extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private Integer maxMembers;

    @OneToMany(mappedBy = "room", cascade = CascadeType.PERSIST)
    private List<User> participants = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "room_banned_users", joinColumns = @JoinColumn(name = "room_id"))
    private Set<Long> bannedUserIds = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY)
    private User manager;

    private Room(String title, Integer maxMembers, User manager) {
        this.title = title;
        this.maxMembers = maxMembers;
        this.manager = manager;
        acceptParticipant(manager, 0);
    }

    public static Room createRoom(String title, Integer maxMembers, User manager) {
        return new Room(title, maxMembers, manager);
    }

    public static Room createForTest(Long roomId, String title, Integer maxMembers, User manager) {
        Room room = new Room(title, maxMembers, manager);
        room.id = roomId;
        return room;
    }

    public void updateRoom(String title, Integer maxMembers) {
        this.title = title;
        this.maxMembers = maxMembers;
    }

    public void acceptParticipant(User user, long currentCount) {
        if (currentCount >= this.maxMembers) {
            throw new BusinessException(ErrorCode.ROOM_MAX_CAPACITY_REACHED);
        }

        this.participants.add(user);
        user.enterRoom(this);
    }

    public void removeParticipant(User user) {
        participants.remove(user);
    }

    public void setBannedUsers(User user) {
        bannedUserIds.add(user.getId());
    }

    public void delegateManager(User user) {
        manager = user;
    }

    public void deleteSetting() {
        List<User> list = new ArrayList<>(this.participants);
        for(User user : list) {     // foreach Iterator에서 원본 리스트 원소의 삭제를 허가하지 않음
            user.leaveRoom();
            participants.remove(user);
        }
    }
}
