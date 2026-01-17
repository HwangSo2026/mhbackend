package com.hwangso.mhbackend.iwtest;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.UUID;


@Testcontainers
@SpringBootTest
public class RedisPractice1 {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisPropertie(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Autowired
    StringRedisTemplate redis;

    @BeforeEach
    void clear() {
        var connection = redis.getConnectionFactory().getConnection();
        connection.serverCommands().flushAll();
    }

    static String holdKey(String date, String slot, String room) {
        return "hold:" + date + ":" + slot + ":" + room;
//        ex } hold:2026-01-06:1:1
    }


    @Test
    @DisplayName("레디스 선점키 생성 테스트")
    void createTokenTest() {
        String token = UUID.randomUUID().toString();
        String holdKey = holdKey("2026-01-06", "1", "1");
        long ttlSeconds = 300;

        System.out.println("ttlSeconds = " + ttlSeconds);
        System.out.println("holdKey = " + holdKey);
        System.out.println("token = " + token);

        // 비어있다면 set 하고 true 반환, 없으면 false
        Boolean ok = redis.opsForValue().setIfAbsent(holdKey, token, Duration.ofSeconds(ttlSeconds));
        System.out.println("ok = " + ok);

        String s = redis.opsForValue().get(holdKey);
        Assertions.assertEquals(token, s);

        // 이제 다른 토큰 접근 하기 원함
        String token2 = UUID.randomUUID().toString();
        String holdKey2 = holdKey("2026-01-06", "1", "1");
        long ttlSeconds2 = 300;

        Boolean b = redis.opsForValue().setIfAbsent(holdKey2, token2, Duration.ofSeconds(ttlSeconds2));

        Assertions.assertNotEquals(s, b); // s는 True 였지만, 이번엔 실패
        System.out.println("b = " + b); // false
    }

    @Test
    @DisplayName("토큰 존재 여부 확인")
    void getTokenTest() {
        String token = UUID.randomUUID().toString();
        String holdKey = holdKey("2026-01-06", "1", "1");
        long ttlSeconds = 300;
        Object ok = redis.opsForValue().setIfAbsent(holdKey, token, Duration.ofSeconds(ttlSeconds));
        System.out.println("typeOf(ok) = " + ok.getClass().getSimpleName());

        Boolean b = redis.hasKey(holdKey);
        if (b) { //이미 있다면,
            System.out.println("이미 존재");
        } else {
            Object ok2 = redis.opsForValue().setIfAbsent(holdKey, token, Duration.ofSeconds(ttlSeconds));
            System.out.println("저장 완료 : "+ok2);
        }
    }




}
