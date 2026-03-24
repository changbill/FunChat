package com.funchat.demo.global.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SystemConstants {
    public static final String ENTER_MENTION = "님이 입장하셨습니다.";
    public static final String EXIT_MENTION = "님이 퇴장하셨습니다.";
    public static final String DELEGATION_MENTION = "님이 방장이 되셨습니다.";
    public static final String BAN_MENTION = "님이 강퇴되었습니다.";

    public static final long USER_ID = 0L;
    public static final String USER_NICKNAME = "SYSTEM";
}
