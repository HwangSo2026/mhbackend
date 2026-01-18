package com.hwangso.mhbackend.common.error;

import com.hwangso.mhbackend.common.error.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // (400/ 01/403/404/409/410 등)

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e, HttpServletRequest req) {
        ErrorCode ec = e.getErrorCode();
        return ResponseEntity
                .status(ec.status)
                .body(new ErrorResponse(
                        ec.status.value(),
                        ec.code,
                        e.getMessage(),              // override 가능
                        req.getRequestURI(),
                        Instant.now(),
                        null
                ));
    }

    // DTO validation 실패 (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<ErrorResponse.FieldViolation> violations = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toViolation)
                .toList();

        String msg = violations.isEmpty() ? "요청 값이 올바르지 않습니다." : "요청 값 검증에 실패했습니다.";
        ErrorCode ec = ErrorCode.INVALID_REQUEST;

        return ResponseEntity
                .status(ec.status)
                .body(new ErrorResponse(
                        ec.status.value(),
                        ec.code,
                        msg,
                        req.getRequestURI(),
                        Instant.now(),
                        violations
                ));
    }

    // JSON 파싱 실패 (400) - 요청 바디 깨짐
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParse(HttpMessageNotReadableException e, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INVALID_REQUEST;

        return ResponseEntity
                .status(ec.status)
                .body(new ErrorResponse(
                        ec.status.value(),
                        ec.code,
                        "요청 본문(JSON)이 올바르지 않습니다.",
                        req.getRequestURI(),
                        Instant.now(),
                        null
                ));
    }

    // 필수 query param 누락 (400)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INVALID_REQUEST;

        return ResponseEntity
                .status(ec.status)
                .body(new ErrorResponse(
                        ec.status.value(),
                        ec.code,
                        "필수 파라미터가 누락되었습니다: " + e.getParameterName(),
                        req.getRequestURI(),
                        Instant.now(),
                        null
                ));
    }

    // 타입 미스매치 (400) - 예: 숫자 넣어야 하는데 문자
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INVALID_REQUEST;

        return ResponseEntity
                .status(ec.status)
                .body(new ErrorResponse(
                        ec.status.value(),
                        ec.code,
                        "요청 값 타입이 올바르지 않습니다: " + e.getName(),
                        req.getRequestURI(),
                        Instant.now(),
                        null
                ));
    }

    // 헤더 누락 Missing gRequestHeaderException
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.UNAUTHORIZED; // 또는 INVALID_REQUEST 정책에 맞게
        return ResponseEntity.status(ec.status).body(new ErrorResponse(
                ec.status.value(), ec.code, "필수 헤더가 누락되었습니다: " + e.getHeaderName(),
                req.getRequestURI(), Instant.now(), null
        ));
    }

    // 마지막 안전망 (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;

        return ResponseEntity
                .status(ec.status)
                .body(new ErrorResponse(
                        ec.status.value(),
                        ec.code,
                        ec.message,
                        req.getRequestURI(),
                        Instant.now(),
                        null
                ));
    }

    private ErrorResponse.FieldViolation toViolation(FieldError fe) {
        return new ErrorResponse.FieldViolation(fe.getField(), fe.getDefaultMessage());
    }
}