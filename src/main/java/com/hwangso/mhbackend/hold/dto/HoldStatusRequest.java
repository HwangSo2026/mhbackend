package com.hwangso.mhbackend.hold.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record HoldStatusRequest(

        @Schema(example = "2026-01-02", description = "예약 날짜 (YYYY-MM-DD)")
        @NotBlank(message = "date는 필수입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "date 형식은 YYYY-MM-DD 이어야 합니다.")
        String date,

        @Schema(example = "part3", description = "슬롯 (part1~part7)")
        @NotBlank(message = "slot은 필수입니다.")
        @Pattern(regexp = "^part[1-7]$", message = "slot은 part1~part7 중 하나여야 합니다.")
        String slot,

        @Schema(example = "4", description = "회의실 번호 (1~7)")
        @NotBlank(message = "room은 필수입니다.")
        @Pattern(regexp = "^[1-7]$", message = "room은 1~7 중 하나여야 합니다.")
        String room

) {
}