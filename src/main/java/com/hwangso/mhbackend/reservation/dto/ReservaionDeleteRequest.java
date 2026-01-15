package com.hwangso.mhbackend.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ReservaionDeleteRequest {

    public record ReservationDeleteRequest(
            @NotBlank
            @Pattern(regexp="^\\d{4}$", message="password는 4자리 숫자여야 합니다.")
            String password
    ) {}
}
