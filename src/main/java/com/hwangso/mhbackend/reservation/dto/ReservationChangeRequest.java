package com.hwangso.mhbackend.reservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * 예약 시간/회의실 변경 요청
 * - 기존 예약 1개 → 새 예약 1~2개로 변경 가능
 * - 내부적으로는 "삭제 + 생성" 트랜잭션
 */
public record ReservationChangeRequest(

        /** 예약자 비밀번호 (기존 예약 검증용) */
        @NotBlank
        @Pattern(regexp = "^\\d{4}$", message = "password는 4자리 숫자여야 합니다.")
        String password,

        /** 기존 예약 정보 */
        @Valid
        FromReservation from,

        /** 변경 후 예약 목록 (1~2개 허용) */
        @Size(min = 1, max = 2)
        @Valid
        List<ToReservation> to
) {

    /**
     * 기존 예약 (삭제 대상)
     */
    public record FromReservation(
            @NotBlank @Pattern(regexp="^\\d{4}-\\d{2}-\\d{2}$")
            String date,

            @NotBlank @Pattern(regexp="^part[1-8]$")
            String slot,

            @NotBlank @Pattern(regexp="^[1-7]$")
            String room
    ) {}

    /**
     * 변경 후 예약 (생성 대상)
     */
    public record ToReservation(
            @NotBlank @Pattern(regexp="^\\d{4}-\\d{2}-\\d{2}$")
            String date,

            @NotBlank @Pattern(regexp="^part[1-8]$")
            String slot,

            @NotBlank @Pattern(regexp="^[1-7]$")
            String room,

            @NotBlank
            String name,

            @NotBlank
            String course,

            @Min(1) @Max(99)
            int headcount
    ) {}
}