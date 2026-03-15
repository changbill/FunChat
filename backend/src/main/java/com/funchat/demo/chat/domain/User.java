package com.funchat.demo.chat.domain;

import com.funchat.demo.room.domain.ChatRoomUser;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nickname;

    @Singular
    @OneToMany(mappedBy = "user")
    private List<ChatRoomUser> chatRoomUsers;
}
