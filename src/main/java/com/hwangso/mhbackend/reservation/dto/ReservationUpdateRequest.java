package com.hwangso.mhbackend.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 수정은 "비밀번호 검증" 기반.
 * 변경 가능한 필드만 둠 (필요 시 추가)
 */
public record ReservationUpdateRequest(
        @NotBlank @Pattern(regexp="^\\d{4}$", message="password는 4자리 숫자여야 합니다.")
        String password,

        String name, //이름
        String course, //반

        @Min(1) @Max(99)
        Integer headcount //인원
) {}