package com.funchat.demo.global.exception;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * Creates an exception with business error code.
     *
     * @param errorCode business error code
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * Creates an exception with business error code and message.
     *
     * @param errorCode business error code
     * @param message error detail message
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Creates an exception with business error code, message and root cause.
     *
     * @param errorCode business error code
     * @param message error detail message
     * @param cause underlying cause
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}