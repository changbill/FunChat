package com.funchat.demo.user.domain;

import com.funchat.demo.global.domain.BaseTimeEntity;
import com.funchat.demo.global.exception.BusinessException;
import com.funchat.demo.global.exception.ErrorCode;
import com.funchat.demo.room.domain.Room;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email; // 로그인 아이디로 사용

    private String password; // 소셜 로그인 시 null 가능
    private String nickname;
    private String profileImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    private LocalDateTime enteredAt;

    @Enumerated(EnumType.STRING)
    private SocialType socialType = SocialType.LOCAL; // TODO: 나머지 GOOGLE, KAKAO 소셜 로그인 구현

    private User(String email, String password, String nickname, String profileImageUrl) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public static User createUser(String email, String password, String nickname, String profileImageUrl) {
        return new User(email, password, nickname, profileImageUrl);
    }

    public static User createForTest(Long userId, String email, String nickname, String profileImageUrl) {
        User user = new User(email, null, nickname, profileImageUrl);
        user.id = userId;
        return user;
    }

    public boolean isRoomManager() {
        return room != null && room.getManager().equals(this);
    }

    public void enterRoom(Room room) {
        if(this.room != null) {
            throw new BusinessException(ErrorCode.ROOM_USER_ALREADY_JOINED);
        }

        this.room = room;
        this.enteredAt = LocalDateTime.now();
    }

    public void leaveRoom() {
        if(this.room == null) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }

        room.removeParticipant(this);
        this.room = null;
        this.enteredAt = null;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof User user)) return false;
        return id != null && id.equals(user.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
