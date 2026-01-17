package com.hwangso.mhbackend.reservation.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final StringRedisTemplate redis;

    private static final Duration TTL = Duration.ofHours(12);

    /**
     * 저장 할 때 프리픽스 admin:token:
     */
    private static final String PREFIX = "admin:token:";

    public String issueToken() {
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(PREFIX + token, "true", TTL); // Key-value 에서 키값만 사용하고싶음 value는 아무개
        return token;
    }

    public boolean isValid(String token) {
        if (token == null || token.isBlank()) return false;
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + token));
    }

    public void revoke(String token) {
        if (token == null || token.isBlank()) return;
        redis.delete(PREFIX + token);
    }

}
