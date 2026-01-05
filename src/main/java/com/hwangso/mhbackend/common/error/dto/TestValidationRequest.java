package com.hwangso.mhbackend.common.error.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TestValidationRequest(

        @NotBlank(message = "date는 필수입니다.")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "date 형식은 YYYY-MM-DD 이어야 합니다.")
        String date,

        @NotBlank(message = "slot은 필수입니다.")
        @Pattern(regexp = "^part[1-7]$", message = "slot은 part1~part7 중 하나여야 합니다.")
        String slot

) {
}