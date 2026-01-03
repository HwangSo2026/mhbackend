package com.hwangso.mhbackend.hold.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Repository
public class HoldRedisRepository {

    private final StringRedisTemplate redis;
    private final RedisScript<Long> holdRefreshScript;

    public HoldRedisRepository(
            StringRedisTemplate redis,
            @Qualifier("holdRefreshScript") RedisScript<Long> holdRefreshScript
    ) {
        this.redis = redis;
        this.holdRefreshScript = holdRefreshScript;
    }

    /** SET holdKey token NX EX ttlSeconds */
    public boolean acquireHold(String holdKey, String token, long ttlSeconds) {
        Boolean ok = redis.opsForValue().setIfAbsent(holdKey, token, Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(ok);
    }


    /** holdKey 존재 여부 */
    public boolean existsHold(String holdKey) {
        Boolean exists = redis.hasKey(holdKey);
        return Boolean.TRUE.equals(exists);
    }

    /** TTL(seconds). 없으면 -2, 만료설정 없으면 -1 */
    public long ttlSeconds(String holdKey) {
        Long ttl = redis.getExpire(holdKey, TimeUnit.SECONDS);
        return ttl == null ? -2 : ttl;
    }

    /**
     * Lua refresh 결과:
     *  1: 성공
     *  0: 토큰 불일치
     * -1: 키 없음(만료)
     */
    public long refreshHold(String holdKey, String token, long ttlSeconds) {
        Long res = redis.execute(
                holdRefreshScript,
                List.of(holdKey),
                token,
                String.valueOf(ttlSeconds)
        );
        return res == null ? 0 : res;
    }

}