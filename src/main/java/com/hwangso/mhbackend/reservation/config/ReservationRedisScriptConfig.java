package com.hwangso.mhbackend.reservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Reservation 도메인에서 사용하는 Redis Lua 스크립트 설정
 * - Create / Update / Delete 용
 */
@Configuration
public class ReservationRedisScriptConfig {

    /**
     * 예약 확정(Create)
     * - hold 검증 + 예약 저장 + hold 해제 + TTL 설정
     * - 반드시 원자적으로 실행되어야 함
     */

    @Bean(name = "reservationCreateScript")
    public RedisScript<Long> reservationCreateScript() {
        String lua = """
            -- KEYS[1] = holdKey
            -- KEYS[2] = rsvKey
            -- ARGV[1] = holdToken
            -- ARGV[2] = room
            -- ARGV[3] = reservationJson
            -- ARGV[4] = ttlSeconds

            local holdVal = redis.call('GET', KEYS[1])
            if not holdVal then
              return -1 -- HOLD_NOT_FOUND
            end
            if holdVal ~= ARGV[1] then
              return 0  -- HOLD_TOKEN_MISMATCH
            end

            local exists = redis.call('HEXISTS', KEYS[2], ARGV[2])
            if exists == 1 then
              return -2 -- ALREADY_RESERVED
            end

            redis.call('HSET', KEYS[2], ARGV[2], ARGV[3])
            redis.call('DEL', KEYS[1])
            redis.call('EXPIRE', KEYS[2], tonumber(ARGV[4]))
            return 1 -- OK
        """;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 예약 삭제(Delete)
     * - 비밀번호 검증은 Java에서 수행
     * - Lua에서는 HDEL만 원자 처리
     */
    @Bean(name = "reservationDeleteScript")
    public RedisScript<Long> reservationDeleteScript() {
        String lua = """
            -- KEYS[1] = rsvKey
            -- ARGV[1] = room

            local current = redis.call('HGET', KEYS[1], ARGV[1])
            if not current then
              return -1 -- NOT_FOUND
            end

            redis.call('HDEL', KEYS[1], ARGV[1])
            return 1
        """;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 예약 수정(Update) 사용 x
     * - 비밀번호 검증은 Java에서 수행
     * - Lua에서는 HSET만 원자 처리
     */
    @Bean(name = "reservationUpdateScript")
    public RedisScript<Long> reservationUpdateScript() {
        String lua = """
            -- KEYS[1] = rsvKey
            -- ARGV[1] = room
            -- ARGV[2] = updatedReservationJson

            local current = redis.call('HGET', KEYS[1], ARGV[1])
            if not current then
              return -1 -- NOT_FOUND
            end

            redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
            return 1
        """;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(Long.class);
        return script;
    }

}