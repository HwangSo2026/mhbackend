package com.hwangso.mhbackend.reservation.dto;

public record ReservationResponse(
        String date,
        String slot,
        String room,
        ReservationData reservation
) {
    public record ReservationData(
            String name,
            String course,
            int headcount
    ) {}
}