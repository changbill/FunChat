package com.funchat.demo.user.domain;

import com.funchat.demo.global.domain.BaseTimeEntity;
import com.funchat.demo.room.domain.RoomUser;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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

    @Enumerated(EnumType.STRING)
    private SocialType socialType = SocialType.LOCAL; // TODO: 나머지 GOOGLE, KAKAO 소셜 로그인 구현

    @Enumerated(EnumType.STRING)
    private UserStatus status; // ONLINE, OFFLINE 등
}
