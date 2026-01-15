package com.hwangso.mhbackend.reservation.dto;

import java.util.List;

public record ReservationSearchResponse(
        String role,           // "admin" | "user"
        String adminToken,     // admin일 때만 (없으면 null)
        List<ReservationResponse> reservations
) {}