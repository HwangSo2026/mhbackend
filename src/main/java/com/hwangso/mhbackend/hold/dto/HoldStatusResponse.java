package com.hwangso.mhbackend.hold.dto;

public record HoldStatusResponse(

        boolean held,

        long ttlSeconds // held=false면 0

) {
}