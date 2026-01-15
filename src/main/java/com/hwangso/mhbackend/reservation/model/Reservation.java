package com.hwangso.mhbackend.reservation.model;

/**
 * Redis Hash value에 JSON으로 저장할 구조.
 * passwordHash는 외부 응답으로 절대 내보내지 않음.
 */
public record Reservation(
        String name,
        String course,
        int headcount,
        String passwordHash
) {}