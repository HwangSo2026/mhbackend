package com.hwangso.mhbackend.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 403 Forbidden
    HOLD_FORBIDDEN(HttpStatus.FORBIDDEN, "HOLD_FORBIDDEN", "홀드 주인이 아닙니다."),
    PASSWORD_MISMATCH(HttpStatus.FORBIDDEN, "PASSWORD_MISMATCH", "비밀번호가 일치하지 않습니다."),

    // 404 Not Found
    HOLD_NOT_FOUND(HttpStatus.NOT_FOUND, "HOLD_NOT_FOUND", "홀드 키가 존재하지 않습니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "예약이 존재하지 않습니다."),

    // 409 Conflict
    HOLD_CONFLICT(HttpStatus.CONFLICT, "HOLD_CONFLICT", "이미 다른 사용자가 작성 중입니다."),
    RESERVATION_CONFLICT(HttpStatus.CONFLICT, "RESERVATION_CONFLICT", "이미 예약되어 있습니다."),

    // 410 Gone
    HOLD_GONE(HttpStatus.GONE, "HOLD_GONE", "홀드가 만료되어 확정할 수 없습니다."),

    // 400 Bad Request
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 값이 올바르지 않습니다."),

    // 500
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");

    public final HttpStatus status;
    public final String code;
    public final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}