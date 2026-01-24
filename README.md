# MeetHub ⏰ ( 회의실 예약 웹 서비스 )
**Redis를 주 저장소로 사용해 “당일 회의실 예약”을 중복 없이 처리하는 예약 웹 서비스입니다.** 

## 🔗 바로가기
- **Service**: [meethub.inwoohub.com](https://meethub.inwoohub.com/)
- **API Docs (Swagger)**: [Swagger UI](https://meethub.inwoohub.com/swagger-ui/index.html)

---
## 0. 목차
> 1.  [👨‍💻 프로젝트 개요](#sec-1-overview)
> 2.  [⚒️ 사용 기술](#sec-2-tech)
> 3.  [🌐 아키텍처](#sec-3-arch)
> 4.  [🖥️ 화면 구성](#sec-4-ui)
> 5.  [📌 주요 기능 설계](#sec-5-feature)
> 6.  [⚠️ 트러블슈팅](#sec-6-trouble)

<br>

<a id="sec-1-overview"></a>
## 1. 👨‍💻 프로젝트 개요
### 1-1. 소개
MeetHub는 칠판에 이름을 적던 회의실 예약을 웹으로 디지털화한 **당일 예약 서비스**입니다.  
Redis를 주 저장소로 사용하고 **Hold + Lua 원자 처리 + TTL(자정 만료)** 로 중복 예약과 일일 초기화를 해결했습니다.

### 1-2. 기간
- **2025.12 ~ 2026.01** (약 1개월)

<br>

### 1-3. 배경
- 칠판 예약: 현황 공유 어려움, 변경/취소 처리 번거로움
- 동일한 기준으로 예약 현황을 공유할 수 있는 웹 기반 예약 방식 필요

<br>

### 1-4. 개발 목표
- 칠판 예약을 웹으로 전환하여 **당일 예약 현황을 한 화면에서 공유**
- **중복 예약/동시성 문제를 0%로 방지**하는 예약 확정 로직 구현
- Redis를 **주 저장소(Primary Store)** 로 사용하며 키 설계/자료구조/TTL 운영 경험
- Hold(선점) 기반 UX와 **Lua Script 원자 처리**로 안정적인 예약 흐름 구현


#### 1-4-1. Redis 선택 이유
MeetHub는 “당일 예약” 특성상 **동시성(중복 예약) 제어**와 **자정 초기화(TTL)** 가 핵심입니다.  
Redis는 `TTL`과 `Lua Script`로 예약 확정 흐름을 단순하게 **원자 처리**할 수 있어 주 저장소로 선택했습니다.  
또한 Redis를 캐시가 아닌 **데이터 저장소 관점(키 설계, TTL, 원자 처리)** 으로 설계·운영해보는 것을 학습 목표로 포함했습니다.

<br>

### 1-5. 팀원 소개

| 황인우 | 김소정 |
| :---: | :---: |
| <img width="163" height="166" alt="인우" src="../docs/images/inwoo.jpg" /> | <img width="163" height="166" alt="소정" src="../docs/images/sojeong.png" /> |
| [Github](https://github.com/inwoohub) | [Github](https://github.com/cowjeong) |
| **BE & FE** | **BE & FE** |
| Redis 키 설계(예약/Hold/TTL) <br/> Hold(선점) <br/> Lua Script(원자 처리) <br/> Admin API <br/> 전역 예외 처리 <br/> 배포(Nginx/HTTPS/도메인) <br/> FE 보조(UI/연동 일부) | 예약 CRUD <br/> 예약 화면/플로우 설계 <br/> Lua Script(원자 처리) <br/> FE 메인(UI/상태관리/연동) |

<br>


<a id="sec-2-tech"></a>
## 2. ⚒️ 사용 기술
<img src="https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=React&logoColor=white"> <img src="https://img.shields.io/badge/java-007396?style=for-the-badge&logo=OpenJDK&logoColor=white"> <img src="https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=Spring&logoColor=white"> <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white"> <img src="https://img.shields.io/badge/docker-%230db7ed?style=for-the-badge&logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/nginx-%23009639.svg?style=for-the-badge&logo=nginx&logoColor=white">

<br>


<a id="sec-3-arch"></a>
## 3. 🌐 아키텍처

### 3-1. 시스템 아키텍처
![Architecture](../docs/images/arch.png)

<br>

### 3-2. Redis 데이터 모델 (키 설계)
- Hold(String) + Reservation(Hash) 구조로 동시성/만료 정책을 구현했습니다.

#### 3-2-1. 선점 키 (Hold)
- Key: `hold:{date}:{slot}:{room}` / Type: String / TTL: 300초

![holdKey](../docs/images/holdKey.png)

<br>


#### 3-2-2. 예약 키 (Reservation)
- Key: `rsv:{date}:{slot}` / Type: Hash / Field: room(1~7) / Value: JSON / TTL: 자정 만료 

![rsvKey](../docs/images/rsvKey.png)

<br>


<a id="sec-4-ui"></a>
## 4. 🖥️ 화면 구성
![Frame01](../docs/images/Frame01.png)
![Frame02](../docs/images/Frame02.png)
![Frame03](../docs/images/Frame03.png)

<br>


<a id="sec-5-feature"></a>
## 5. 📌 주요 기능 설계
### 5-1. 전역 예외 처리
![GlobalException](../docs/images/GlobalException.png)
MeetHub API는 에러 응답 포맷을 항상 동일하게 내려주기 위해 전역 예외 처리 구조를 사용합니다.

#### 5-1-1. 목표

- Controller/Service에서 예외가 발생해도 한 곳에서 처리
- HTTP 상태코드/에러코드/메시지를 표준화
- 클라이언트는 항상 동일한 JSON 구조 (ErrorResponse)로 에러 처리가 가능


#### 5-1-2. 구조

1. ErrorCode (에러 정책 중앙화)

    에러 상황별로 HTTP status + 내부 코드 + 기본 메시지를 enum으로 정의합니다.

    - ex )
    ```java
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "예약이 존재하지 않습니다."),
    ```
    

<br>

2. ApiException (ErrorCode를 담아 던지는 커스텀 예외)
    비즈니스 로직에서 에러가 발생하면 다음처럼 ErrorCode를 포함한 예외를 던집니다.

    - ex )
    ```java
    throw new ApiException(ErrorCode.RESERVATION_NOT_FOUND);   
    ```

<br>


3. @RestControllerAdvice (예외 → ErrorResponse 변환)
    요청 처리 중 발생한 예외는 DispatcherServlet을 거쳐 전파되고, @RestControllerAdvice의 @ExceptionHandler가 예외를 잡아 ErrorResponse로 매핑합니다.

    - ApiException → ErrorCode를 꺼내서 상태코드/코드/메시지 응답 생성
    - Validation / JSON / 파라미터 누락 등의 스프링 예외도 공통 포맷으로 변환

<br>

4. 응답 예시

    ```json
    {
        "status": 404,
        "code": "HOLD_NOT_FOUND",
        "message": "홀드 키가 존재하지 않습니다.",
        "path": "/api/test/errors/404",
        "timestamp": "2026-01-22T11:34:41.688975599Z",
        "violations": null
    }
    ```

#### 5-1-3. 관련 코드
- [GlobalExceptionHandler.java](https://github.com/HwangSo2026/mhbackend/blob/main/src/main/java/com/hwangso/mhbackend/common/error/GlobalExceptionHandler.java)
- [ErrorCode.java](https://github.com/HwangSo2026/mhbackend/blob/main/src/main/java/com/hwangso/mhbackend/common/error/ErrorCode.java)
- [ApiException.java](https://github.com/HwangSo2026/mhbackend/blob/main/src/main/java/com/hwangso/mhbackend/common/error/ApiException.java)

<br>

---

### 5-2. 선점 기능 (Hold)
![holdFlow](../docs/images/holdFlow.png)

사용자가 예약 정보를 입력하는 "작성 단계"에서 동시에 같은 슬롯을 클릭할 수 있기 때문에, 예약 확정 이전에 선점(Hold)을 걸어 중복 예약을 차단합니다.
Hold는 Redis의 String + TTL로 구현하며 토큰(token) 으로 소유자를 식별해 타인이 연장/해제하지 못하도록 보호합니다.

#### 5-2-1. 목표
- 동일 날짜/슬롯/회의실에 대해 동시에 여러 요청이 와도 1명이 선점
- 작성 중 이탈/방치 케이스는 TTL로 자동 해제하여 자원 회전율 확보
- Hold 소유자만 Refresh/Release 가능 하도록 토큰 검증 적용
- Refresh/Release는 Lua Script로 원자처리 하여 레이드 컨디션 방지

#### 5-2-2. 구조
1. Hold Key (String + TTL)
    - Key:hold:{date}:{slot}:{room}
    - Value : token
    - TTL : 300s

<br>

2. Acquire Hold (선점 요청)
    - Redis 명령 : SET holdKey token NX EX 300
    - NX 조건으로 키가 없을 때만 생성되므로 이미 누군가 선점 중이면 실패합니다.
    - 실패 시 409 HOLD_CONFLICT 로 응답합니다.

<br>

3. Refresh Hold (선점 연장)
    - 작성 시간이 더 필요할 때 TTL을 연장합니다.
    - GET과 EXPIRE를 분리하면 중간에 값이 바뀌는 레이스가 발생할 수 있어 Lua Script로 토큰 검증 + TTL 연장을 Redis 내부에서 수행합니다.
    - 반환값 정책
        - 1 → 200 REFRESHED
        - -1 → 404 HOLD_NOT_FOUND (키 없음/만료)
        - 0 → 403 HOLD_FORBIDDEN (토큰 불일치)

<br>

4. Release Hold (선점 해제)
    - 취소/뒤로가기 등으로 작성이 중단되면 Hold를 해제합니다.
    - Refresh와 동일하게 Lua Script로 토큰 검증 + DEL을 원자 처리합니다.
    - 반환값 정책
        - 1 → 200 RELEASED
        - -1 → 404 HOLD_NOT_FOUND
        - 0 → 403 HOLD_FORBIDDEN


#### 5-2-3. 관련 코드
- [HoldRedisScriptConfig.java](https://github.com/HwangSo2026/mhbackend/blob/main/src/main/java/com/hwangso/mhbackend/hold/config/HoldRedisScriptConfig.java)
- [HoldRedisRepository.java](https://github.com/HwangSo2026/mhbackend/blob/main/src/main/java/com/hwangso/mhbackend/hold/repository/HoldRedisRepository.java)
- [HoldService.java](https://github.com/HwangSo2026/mhbackend/blob/main/src/main/java/com/hwangso/mhbackend/hold/service/HoldService.java)
- [HoldController.java](https://github.com/HwangSo2026/mhbackend/blob/main/src/main/java/com/hwangso/mhbackend/hold/controller/HoldController.java)

<br>

---

### 5-3. 예약 기능
![reservationFlow](../docs/images/rsvFlow.png)

예약은 Redis를 **주 저장소(Primary Store)** 로 사용하며, 슬롯 단위 Hash 구조로 예약을 저장합니다.  
예약 확정(Create)은 **Hold 검증 + 중복 검사 + 저장 + Hold 해제 + TTL**이 하나의 트랜잭션이므로, Lua Script로 **원자 처리**하여 중복 예약을 방지합니다.

#### 5-3-1. 목표
- Hold를 선점한 사용자만 예약 확정 가능
- 동일 날짜/슬롯/회의실에 대해 **중복 예약 0%**
- 예약 데이터는 **자정(TTL) 기준 자동 만료**되어 일일 초기화 배치 없이 운영
- 비밀번호는 bcrypt hash만 저장하고, 삭제/변경은 비밀번호 검증 필수
- “수정(Update)” 대신 “변경(Change)”을 **삭제 + 재생성**으로 단순화

#### 5-3-2. 구조
1. Reservation Key (Hash + TTL)
   - Key: `rsv:{date}:{slot}`
   - Type: Hash
   - Field: `room` (1~7)
   - Value: `JSON` (password는 bcrypt hash로 저장)
   - TTL: 자정 만료 (다음날 00:00)

<br>

2. Create (예약 확정) — Lua Script 원자 처리
   - 처리 흐름
     1) `GET holdKey` (Hold 존재 확인)
     2) token 검증 (Hold 소유자 확인)
     3) `HEXISTS rsvKey room` (중복 예약 확인)
     4) `HSET rsvKey room json` (예약 저장)
     5) `DEL holdKey` (Hold 해제)
     6) `EXPIRE rsvKey ttl` (자정 만료 TTL 적용)

   - 반환값 정책
     - 1  → 200 RESERVED (성공)
     - -1 → 404 HOLD_NOT_FOUND (Hold 만료/미생성)
     - 0  → 403 HOLD_FORBIDDEN (토큰 불일치)
     - -2 → 409 RESERVATION_CONFLICT (이미 예약됨)

<br>

3. Read (예약 조회)
   - HGETALL rsvKey로 슬롯 전체를 조회한 뒤 `name + passwordEncoder.matches` 조건을 만족하는 예약만 반환합니다.
   - 응답 DTO에는 passwordHash를 포함하지 않습니다.

<br>

4. Delete (예약 삭제)
   - 비밀번호 검증은 Java에서 수행하고, Redis에서는 Lua Script로 `(존재 확인 + HDEL)`을 원자적으로 처리합니다.

<br>

5. Change (시간/회의실 변경)
   - slot/room 변경은 Key/Field 이동이 필요해 단일 Update로 처리하면 복잡도가 증가합니다.
   - 따라서 Change는 **삭제 + 재예약** 모델로 단순화했습니다.
     - (1) 기존 예약 조회 + 비밀번호 검증
     - (2) 기존 예약 삭제
     - (3) 새 슬롯/회의실에 대해 Hold → Create 흐름으로 재예약

<br>

#### 5-3-3. 관련 코드
- [ReservationRedisScriptConfig.java](https://github.com/HwangSo2026/mhbackend/blob/main/src/main/java/com/hwangso/mhbackend/reservation/config/ReservationRedisScriptConfig.java)
- [ReservationRedisRepository.java](https://github.com/HwangSo2026/mhbackend/blob/main/src/main/java/com/hwangso/mhbackend/reservation/repository/ReservationRedisRepository.java)
- [ReservationService.java](https://github.com/HwangSo2026/mhbackend/blob/main/src/main/java/com/hwangso/mhbackend/reservation/service/ReservationService.java)
- [ReservationController.java](https://github.com/HwangSo2026/mhbackend/blob/main/src/main/java/com/hwangso/mhbackend/reservation/controller/ReservationController.java)

<br>

---

### 5-4. 관리자 기능 (Admin)
![adminFlow](../docs/images/adminFlow.png)

관리자 기능은 운영 중 발생하는 예약/취소 상황에 대응하기 위해 예약 현황 조회 및 강제 취소 API를 제공합니다.
관리자 인증은 DB없이 환경 변수 기반 로그인 + Redis 토큰으로 단순화하여 운영 편의성을 확보했습니다.

#### 5-4-1. 목표
- 운영 목적상 관리자 전용 조회/강제 취소 기능 제공
- 관리자 계정은 DB가 아닌 환경변수(ADMIN_NAME / ADMIN_PASSWORD) 로 관리
- 로그인 성공 시 Redis에 토큰을 발급/저장하고, 이후 요청은 X-Admin-Token 헤더로 검증
- 만료/위조 토큰은 401 UNAUTHORIZED로 차단하여 관리자 API를 보호

#### 5-4-2. 구조
1. Admin Login (ENV 기반 인증 + 토큰 발급)
   - POST /api/admin/login에서 {name, password}를 입력받아 환경변수와 비교해 검증합니다.
   - 성공 시 UUID 토큰을 발급하고 Redis에 저장한 뒤 { role: "admin", token }을 반환합니다.

<br>

2. Admin Token (Redis String + TTL)
   - Key: admin:token:{token}
   - Type: String
   - Value: "true" (키 존재 여부만 필요)
   - TTL: 12시간

<br>

3. Admin API Guard (헤더 검증)
   - 관리자 API 요청 시 X-Admin-Token: {token} 헤더를 요구합니다.
   - 검증 성공 시 관리자 전용 API를 수행합니다.
        - readAll: 슬롯 전체 예약 조회
        - forceDelete: 특정 예약 강제 취소
        - readRoomAllSlots: 특정 회의실의 하루 전체 슬롯 조회

<br>

#### 5-4-3. 관련 코드
- [ReservationService.java](https://github.com/HwangSo2026/mhbackend/blob/main/src/main/java/com/hwangso/mhbackend/reservation/service/ReservationService.java)
- [AdminService.java](https://github.com/HwangSo2026/mhbackend/blob/main/src/main/java/com/hwangso/mhbackend/reservation/admin/AdminService.java)
- [AdminController.java](https://github.com/HwangSo2026/mhbackend/blob/main/src/main/java/com/hwangso/mhbackend/reservation/admin/AdminController.java)

<br>


<a id="sec-6-trouble"></a>
## 6. ⚠️ 트러블슈팅

### 6-1. 인우

<details>
<summary> 보기
</summary>

## **1. Admin API 401 (Unauthorized) - 토큰 검증 실패 (토큰 덮어쓰기)**

### 1. 증상

- 로그인은 성공하고 토큰도 발급되었지만, 특정 시점에 관리자 API가 401이 발생했습니다.
- 특히 여러 번 로그인하거나 (새 토큰 발급), 여러 탭/브라우저에서 재현했습니다.

<br>

### 2. 원인

처음에는 토큰을 **값(value)에 저장하고, 키(key)는 고정 prefix만 사용하는 방식으로 구현했습니다.**

- ex)
    - 저장 : `SET admin:token: “{token}” EX 12h`
    - 검증 : `GET admin:token:` 으로 저장된 토큰을 꺼낸 뒤, 요청 헤더 토큰과 비교

이 방식은 “관리자는 1명이면 충분하다”라는 가정에서는 좋다고 느꼈지만, 실제로는 새 토큰이 발급될 때 마다 같은 키를 덮어써서 이전 토큰이 즉시 무효화됩니다.

즉,

- A(admin) 로그인 → tokenA 저장
- B(admin) 로그인 → tokenB 가 같은 키에 덮어씀
- A(admin) api호출 → tokenB가 최신이기 때문에 검증 실패 → 401 Error

<br>

### 3. 해결

토큰을 “값”이 아니라 키 자체에 포함 시키고, 검증은 hasKey로 변경했습니다.

```java
// 저장
redis.opsForValue().set(PREFIX + token, "true", TTL);

// 검증
Boolean.TRUE.equals(redis.hasKey(PREFIX + token));

```

<br>

Value는 의미가 없고 (존재 여부만 필요), “true” 같은 더미 값을 사용합니다.

### 4. 배운 점

- Redis에서 인증 토큰을 관리할 때 “고정 키 + 값 비교”는 덮어쓰기 위험이 있어 확장성 및 안정성이 떨어집니다.
- 토큰 기반 인증은 `admin:token:{token}` 처럼 키로 토큰을 식별하면 단순하고 안전합니다.

<br>

---

## **2. Redis Lua Script Bean 주입 실패 - Spring DI + @Qualifier로 해결**

### 1. 배경

- /gradlew bootRun 실행 시 애플리케이션이 뜨지 않고 DI실패가 발생했다.
- `No qualifying bean of type 'DefaultRedisScript<Long>' available`
- 즉, HoldRedisRepository가 주입받아야 하는 Lua Script Bean을 스프링이 찾지 못했습니다.

<br>

### 2. 원인

Repository에서 Lua Script를 생성자 주입으로 받는 구조였는데, 스프링 컨테이너에 해당 타입 Bean이 없어서 주입이 실패했습니다.

```java
public HoldRedisRepository(
        StringRedisTemplate redis,
        DefaultRedisScript<Long> holdRefreshScript
) {
    this.redis = redis;
    this.holdRefreshScript = holdRefreshScript;
}
```

<br>

또한 Lua Script가 여러 개로 확장될 가능성이 있어, 타입만으로는 모호해질 수 있습니다.

### 3. 해결

Lua Script를 이름 + 타입을 명확히 해서 Bean 등록하고, Repository에서는 @Qualifier로 정확히 주입받도록 변경했습니다.

```java
@Configuration
public class RedisConfig {

    @Bean(name = "holdRefreshScript")
    public RedisScript<Long> holdRefreshScript() {
        String lua = """
            local v = redis.call('GET', KEYS[1])
            if not v then return -1 end
            if v ~= ARGV[1] then return 0 end
            redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
            return 1
        """;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(Long.class);
        return script;
    }
}

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
}
```

<br>

### 4. 배운 점

- Lua Script를 Bean으로 관리할 때는 @Bean 이름 + 반환 타입을 명확히 하는 것이 안전합니다.
- Repository에서는 @Qualifier로 어떤 스크립트를 주입받는지 명확히 해야 DI 문제가 해결가능합니다.

<br>

---

## **3. DB 없이 관리자 계정 관리 (Docker 빌드/실행 시 환경변수 주입)**

### 1. 배경

Redis를 주 저장소로 사용하면서 별도 RDB가 없었기 때문에 관리자 계정을 어디에 저장/관리할지 고민이 있었습니다.

또한 로컬/배포 환경에 따라 관리자 계정이 달라질 수 있어 코드에 하드코딩을 할 수 없었습니다.

<br>

### 2. 해결

관리자 계정은 환경변수 (ADMIN_NAME / ADMIN_PASSWORD) 로 주입받고, Docker 배포 시 compose.yaml의 environment로 주입하도록 구성했습니다.

<br>

### 3. 배운 점

- RDB 없이도 운영용 계정은 환경변수 + 배포 주입 방식으로 안전하게 관리할 수 있습니다.
- 코드 변경 없이 배포 환경에서만 설정 변경이 가능해 운영이 편해집니다.
</details>

<br>

### 6-2. 소정
<details>
<summary>보기
</summary>

## **1. 예약된 회의실인데 “예약 중”이 안 뜨는 문제**

Hold 기반 /api/holds/rooms-status가 확정 예약 (Reservation)을 반영하지 못해, 이미 예약된 방을 “빈 방”으로 판단했습니다.

### 1. 배경

Meethub는 “작성 중 임시 잠금(Hold)” 과 “확정 예약(Reservation)”이 분리된 구조입니다.

- HoldController : 작성 중 선점(Hold) / TTL 기반
    - /api/holds
    - /api/holds/rooms-status
- ReservationController : 확정된 예약 데이터
    - /api/reservations

<br>

### 2. 문제 상황

프론트에서 회의실 목록을 렌더링할 때, 이미 예약이 확정된 방이어도 “예약 중(선택 불가)” 표시가 되지 않는 문제가 발생했습니다.

결과적으로 사용자가 **이미 예약된 방을 클릭할 수 있는 UI 버그**가 발생했습니다.

<br>

### 3. 원인

`/api/holds/rooms-status` 는 이름 그대로 Hold(선점) 상태만 조회하는 API 였습니다.

즉, 기존 API가 보는 범위는

- ✅ 누가 선점 중인지 (hold 존재 여부)
- ❌ 이미 예약 확정된 방 (reservation 존재 여부)는 고려하지 않음

하지만 프론트에서 hold의 의미는 “선점 중인가?”가 아닌, “지금 선택 불가인가?” 였습니다.

| 상태 | 프론트 기대 |
| --- | --- |
| 이미 예약됨 | held = true |
| 누군가 선점 중 | held = true |
| 아무도 안 씀 | held = false |

즉, 백엔드는 “hold 조회 API”와 프론트의 “선택 가능 여부” 요구사항이 서로 달랐습니다.

<br>

### 4. 해결

/api/holds/rooms-status의 책임을 다음처럼 재정의 했습니다.

> “이 슬롯에서 각 회의실이 선택 가능한 상태인지(blocked 여부)를 반환한다.”
> 

따라서 백엔드는 hold 뿐 아니라 reservation도 함께 검사하여, 아래 조건을 적용했습니다.

- blocked = holdExists || reservationExists

```java
boolean holdExists = repo.existsHold(holdKey);
boolean reservationExists = reservationRepo.getReservation(rsvKey, room).isPresent();
boolean blocked = holdExists || reservationExists;
```

<br>

### 5. 한 줄 결론

문제는 CRUD 때문이 아니라, API가 제공하는 의미 (hold 상태)와 프론트가 기대한 의미 (선택 가능 여부)가 달라서 발생한 책임/도메인 정의 문제였습니다.

<br>

---

## **2. 예약 조회 문제**

### 1. 배경

초기 구현에서는 slot에 여러 예약이 있을 때, 이름이 다른 예약을 먼저 만나면 즉시 실패 처리되어 **내 예약이 있어도 조회 실패가 발생했습니다.**

<br>

### 2. 원인

- Redis 구조상 rsv:{date}:{slot}에 여러 room 예약이 들어갈 수 있음
- 기존 로직이
    - 첫 번째 예약만 기준으로 검증하거나
    - name/password 불일치 시 즉시 실패 처리
    → 결과적으로 slot 단위 데이터 구조와 개인 예약 조회 요구사항이 충돌했습니다.

<br>

### 3. 해결

- HGETALL rsvKey로 slot 전체 예약을 조회한 뒤
- 이름이 일치하는 예약만 필터링하고,
- 그 대상에 대해서만 비밀번호 검증을 수행했습니다.
- 이름 불일치는 “에러” 가 아니라 검색 조건 불일치(skip) 로 처리했습니다.

```java
// 이름 불일치 → skip
if (!r.name().equals(name)) return null;

// 비밀번호 불일치 → skip
if (!passwordEncoder.matches(password, r.passwordHash())) return null;
```

→ 결과 : 같은 slot에 여러 예약이 있어도 본인 예약만 안정적으로 조회 가능

<br>

### 4. UX 처리 원칙 (이름 오타)

서버 입장에서는 아래 상황이 모두 “조건에 맞는 예약 없음”으로 동일하게 보입니다.

- 이름 오타 / 다른 날짜,시간 / 예약 자체 없음

또한 이름 존재 여부를 구체적으로 알려주면 정보 노출 위험이 생길 수 있어서 서버는 “예약을 찾을 수 없음”만 반환하고, 프론트에서 가능성 안내 메시지로 처리했습니다.

- ex) “입력한 정보로 예약을 찾을 수 없습니다.”

<br>

### **5. 한 줄 결론**

예약 조회 문제는 “오타”가 아닌, 다중 예약 구조(Hash)에서 조회 책임을 **필터링 방식으로 재정의 하지 못한 설계 문제였습니다.**

</details>




<br>
