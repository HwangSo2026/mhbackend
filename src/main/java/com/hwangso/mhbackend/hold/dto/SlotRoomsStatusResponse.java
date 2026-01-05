package com.hwangso.mhbackend.hold.dto;

import java.util.List;

public record SlotRoomsStatusResponse(
        String date,
        String slot,
        List<RoomHoldStatus> rooms
) {
}