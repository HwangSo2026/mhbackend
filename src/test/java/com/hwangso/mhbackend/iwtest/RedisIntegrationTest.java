package com.hwangso.mhbackend.iwtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers //테스트용으로 Docker 컨테이너 띄움
@SpringBootTest
class RedisIntegrationTest {

    @Container
    static GenericContainer<?> redisContainer =
            new GenericContainer<>("redis:7-alpine") //redis:7-alpine 이라는 이름으로 띄움
                    .withExposedPorts(6379); //컨테이너 안의 6379 포트 열어줌

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    } //위에서 띄웠던 Redis 컨테이너의 실제 host/port 를 스프링 설정으로 주입해서 위에서 띄운 Redis의 접근

    @Autowired
    StringRedisTemplate redis; // StringRedisTemplate 사용

    @BeforeEach
    void clear() {
        var conn = redis.getConnectionFactory().getConnection();
        conn.serverCommands().flushAll(); // 미리 한번 싹 비우기
    }

    @Test
    @DisplayName("데이터 set 하고, get Test")
    void setAndGet() {
        redis.opsForValue().set("레디스 Hello", "World");

        String get1 = redis.opsForValue().get("레디스 Hello");
        System.out.println("get1 = " + get1);
        assertEquals("World", get1);
    }

    @Test
    @DisplayName("HashSetGetTest")
    void setAndGetHash() {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("인우", "레디스테스트");
        String test1 = hashMap.get("인우");

        redis.opsForHash().put("인우", "목적", "레디스학습");
        redis.opsForHash().put("인우", "날짜", "1월2일");

        //단건 조회
        Object o1 = redis.opsForHash().get("인우", "목적");
        Object o2 = redis.opsForHash().get("인우", "날짜");
        System.out.println("redisGet 결과 : " + o1);
        System.out.println("redisGet 결과 : " + o2);

        // 모두 조회
        Map<Object, Object> 인우 = redis.opsForHash().entries("인우");
        System.out.println(인우);

        System.out.println("레디스로 타입 조회 : " + redis.type("인우"));
        System.out.println("자바로 타입 조회" + 인우.getClass());

    }

    @Test
    @DisplayName("예약 hold 테스트")
    void hold() {

        String key = "hold:2026-01-02:part:1";

        //setIfAbsent = 키가 없다면 저장 후 true 반환, 있으면 저장x flase 반환
        Boolean ok1 = redis.opsForValue().setIfAbsent(key, "토큰A", Duration.ofSeconds(6));

        System.out.println("ok1 = " + ok1);
        String 원래키 = redis.opsForValue().get(key);
        System.out.println("원래키 = " + 원래키);

//        Thread.sleep(3000);
        //TTL 확인
        Long ttl = redis.getExpire(key);
        Boolean ok2 = redis.opsForValue().setIfAbsent(key, "토큰B", Duration.ofSeconds(6));
        String 현재키 = redis.opsForValue().get(key);
        System.out.println("현재키 = " + 현재키);
        if (ok2) {
            ttl = redis.getExpire(key);
            System.out.println(현재키 + " 남은 시간= " + ttl);
        } else {
            System.out.println("이미 점유 중 입니다.");
        }
    }

    @Test
    @DisplayName("토큰 발급 테스트")
    void getToken() {
        String token1 = UUID.randomUUID().toString();
        String token2 = UUID.randomUUID().toString();

        System.out.println("token1 = " + token1);
        System.out.println("token2 = " + token2);

        String key = "hold:2026-01-02:part:1";
        redis.opsForValue().set(key, token1, Duration.ofSeconds(6));

        String token = redis.opsForValue().get(key);
        System.out.println(key + "는 현재 점유 토큰 : " + token);

    }

    @Test
    @DisplayName("관리자 테스트")
    void admindTest() {
        // 예약 내역 조회 ( 예약자 명, 비밀번호 )
        class User {
            String name;
            String password;

            User(String name, String password) {
                this.name = name;
                this.password = password;
            }
        }
        User admin = new User("admin", "1234"); // .env 에 넣어둘 예정

        User user1  = new User("박지성", "1234");
        System.out.print("첫 번째 유저 : ");
        if (user1.name.equals(admin.name) && user1.password.equals(admin.password)) {
            System.out.println("관리자 권한 부여 (전체 /조회+삭제/)");
        } else {
            System.out.println("일반 유저 권한 부여 (단일 /조회+수정+삭제/)");
        }

        User user2 = new User("admin", "1234");
        System.out.print("두 번째 유저 : ");
        if (user2.name.equals(admin.name) && user2.password.equals(admin.password)) {
            System.out.println("관리자 권한 부여 (전체 /조회+삭제/)");
        } else {
            System.out.println("일반 유저 권한 부여 (단일 /조회+수정+삭제/)");
        }

    }
}

