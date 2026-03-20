package com.funchat.demo.global.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ErrorCode {

    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "예상치 못한 오류가 발생했습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 사용자를 찾을 수 없습니다."),

    // 채팅방
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 채팅방을 찾을 수 없습니다."),
    ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 채팅방에 접근 권한이 없습니다."),
    ROOM_NOT_MANAGER(HttpStatus.FORBIDDEN, "방장만 수행할 수 있는 작업입니다."),
    ROOM_MAX_CAPACITY_REACHED(HttpStatus.CONFLICT, "채팅방 인원이 가득 찼습니다."),
    ROOM_TITLE_INVALID(HttpStatus.BAD_REQUEST, "채팅방 제목 형식이 올바르지 않습니다."),

    // 참여자
    ROOM_USER_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참여 중인 채팅방입니다."),
    ROOM_USER_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "해당 채팅방의 참여자가 아닙니다."),
    ROOM_USER_BANNED(HttpStatus.FORBIDDEN, "강퇴당한 방에는 다시 입장할 수 없습니다."),
    ROOM_USER_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "참여 가능한 최대 방 개수를 초과했습니다."),

    // ACCESS TOKEN
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Access 토큰입니다."),
    ALREADY_LOGOUT_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "이미 로그아웃된 토큰입니다."),
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 Access 토큰입니다."),
    UNSUPPORTED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "지원되지 않는 Access 토큰 형식입니다."),
    ACCESS_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Access 토큰이 존재하지 않습니다."),

    // REFRESH TOKEN
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다."),
    UNSUPPORTED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "지원되지 않는 리프레시 토큰 형식입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 존재하지 않습니다."),

    // 회원가입
    EMAILS_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다."),
    NICKNAME_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "이미 존재하는 닉네임입니다."),

    // 로그인
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND,"가입되지 않은 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),

    // 인증/인가
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    OAUTH2_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "소셜 로그인 인증에 실패했습니다."),


    // 메시지
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."),
    MESSAGE_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "메시지 전송에 실패했습니다."),
    MESSAGE_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "메시지 길이가 제한을 초과했습니다."),
    MESSAGE_NOT_SENDER(HttpStatus.FORBIDDEN, "자신이 보낸 메시지만 삭제/수정할 수 있습니다."),

    // 요청 제한
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.")
    ;

    private final HttpStatus status;
    private final String message;
}
