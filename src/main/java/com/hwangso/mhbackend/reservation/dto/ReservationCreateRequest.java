package com.hwangso.mhbackend.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * 예약 확정(Create) 요청 DTO
 * - holdToken을 통해 선점 검증
 * - password는 bcrypt로 해시되어 Redis에 저장됨
 */

public record ReservationCreateRequest(

        @Schema(example = "2026-01-02")
        @NotBlank @Pattern(regexp="^\\d{4}-\\d{2}-\\d{2}$")
        String date,

        @Schema(example = "part3")
        @NotBlank @Pattern(regexp="^part[1-8]$")
        String slot,

        @Schema(example = "4")
        @NotBlank @Pattern(regexp="^[1-7]$")
        String room,

        @NotBlank
        String holdToken,

        @NotBlank
        String name,

        @NotBlank
        String course,

        @Min(1) @Max(99)
        int headcount,

        @NotBlank @Pattern(regexp="^\\d{4}$", message="password는 4자리 숫자여야 합니다.")
        String password
) {}