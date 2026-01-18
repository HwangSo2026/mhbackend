package com.hwangso.mhbackend.reservation.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwangso.mhbackend.reservation.model.Reservation;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

/**
 * Reservation 도메인의 Redis 접근 전용 Repository
 * - Redis Hash 구조 사용
 * - JSON 직렬화/역직렬화 책임 포함
 */

@Repository
public class ReservationRedisRepository {

    private final StringRedisTemplate redis;
    private final ObjectMapper om;

    public ReservationRedisRepository(StringRedisTemplate redis, ObjectMapper om) {
        this.redis = redis;
        this.om = om;
    }

    /**
     * formJson(json) : json 문자열을 Reservation 객체로 역 직렬화
     */
    // [수정] JSON 역직렬화 책임을 Repository로 이동
    public Reservation fromJson(String json) {
        try {
            return om.readValue(json, Reservation.class);
        } catch (Exception e) {
            throw new IllegalStateException("예약 JSON 파싱 실패", e);
        }
    }

    public Optional<Reservation> getReservation(String rsvKey, String room) {
        String json = (String) redis.opsForHash().get(rsvKey, room);
        if (json == null) return Optional.empty();
        return Optional.of(fromJson(json));
    }

    public Map<Object, Object> getAll(String rsvKey) {
        return redis.opsForHash().entries(rsvKey);
    }

    public String toJson(Reservation reservation) {
        try {
            return om.writeValueAsString(reservation);
        } catch (Exception e) {
            throw new IllegalStateException("예약 JSON 직렬화 실패", e);
        }
    }


}