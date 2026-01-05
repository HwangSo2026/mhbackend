package com.hwangso.mhbackend.hold.dto;

public record RoomHoldStatus(
        String room,        // "1"~"7"
        boolean held,
        long ttlSeconds     // held=false면 0
) {
}