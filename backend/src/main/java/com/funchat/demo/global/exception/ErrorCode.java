package com.funchat.demo.global.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ErrorCode {
    // 400(BAD_REQUEST)
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 데이터 검증 실패"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 잘못되었습니다. 재확인 바랍니다."),

    // 401(UNAUTHORIZED)

    // 500(INTERNAL_SERVER_ERROR)
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "시스템 오류가 발생했습니다. QnA로 문의 바랍니다."),
    ;

    private final HttpStatus status;
    private final String message;
}
