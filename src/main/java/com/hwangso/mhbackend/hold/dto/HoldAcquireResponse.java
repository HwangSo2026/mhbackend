package com.hwangso.mhbackend.hold.dto;

public record HoldAcquireResponse(

        String holdToken,

        long expiresInSeconds

) {
}