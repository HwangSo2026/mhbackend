package com.hwangso.mhbackend.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class RedisSessionTestController {

    private final StringRedisTemplate redisTemplate;

    /**
     * 1) 세션에 값 저장 (HttpSession)
     * - 이게 Redis 세션 저장소로 잘 들어가면, 서버 재시작해도(세션 TTL 내) 유지되는지 확인 가능
     */
    @PostMapping("/session/set")
    public Map<String, Object> setSession(
            HttpSession session,
            @RequestParam(defaultValue = "hello") String value
    ) {
        session.setAttribute("myValue", value);

        return Map.of(
                "message", "session saved",
                "sessionId", session.getId(),
                "savedValue", value
        );
    }

    /**
     * 2) 세션 값 조회
     */
    @GetMapping("/session/get")
    public Map<String, Object> getSession(HttpSession session) {
        Object v = session.getAttribute("myValue");

        return Map.of(
                "sessionId", session.getId(),
                "myValue", v
        );
    }

    /**
     * 3) 세션 삭제(무효화)
     */
    @PostMapping("/session/invalidate")
    public Map<String, Object> invalidate(HttpSession session) {
        String id = session.getId();
        session.invalidate();
        return Map.of(
                "message", "session invalidated",
                "oldSessionId", id
        );
    }

    /**
     * 4) Redis에 직접 키 저장(세션 말고, Redis 연결 자체 테스트)
     */
    @PostMapping("/redis/set")
    public Map<String, Object> redisSet(
            @RequestParam(defaultValue = "test:key") String key,
            @RequestParam(defaultValue = "123") String value
    ) {
        redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(10));
        return Map.of(
                "message", "redis key saved",
                "key", key,
                "value", value,
                "ttl", "10 minutes"
        );
    }

    /**
     * 5) Redis 키 조회
     */
    @GetMapping("/redis/get")
    public Map<String, Object> redisGet(
            @RequestParam(defaultValue = "test:key") String key
    ) {
        String v = redisTemplate.opsForValue().get(key);
        Long ttl = redisTemplate.getExpire(key);

        return Map.of(
                "key", key,
                "value", v,
                "ttlSeconds", ttl
        );
    }
}