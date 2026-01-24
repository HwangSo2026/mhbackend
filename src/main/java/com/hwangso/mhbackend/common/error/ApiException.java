package com.hwangso.mhbackend.common.error;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;


    public ApiException(ErrorCode errorCode) {
        super(errorCode.message);
        this.errorCode = errorCode;
    }

    /**
     * 원인 추적용 생성자
     * -> 해당 에러시 실제 에러 확인 가능용도
     *
     * Throwable
     * * Exception
     * * * RuntimeException (unchecked)
     * * * 그 외 checked Exception ... 등
     *
     * 해당 프로젝트에서는 로그 추적은 안 할 예정
     * 이런게 있다 정도.
     * 사용처 : 예약 레포지토리
     *
     */
    public ApiException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

}