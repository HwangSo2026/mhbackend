package com.hwangso.mhbackend.reservation.service;

import com.hwangso.mhbackend.reservation.dto.*;
import com.hwangso.mhbackend.reservation.model.Reservation;
import com.hwangso.mhbackend.reservation.repository.ReservationRedisRepository;
import com.hwangso.mhbackend.support.RedisKeys;
import com.hwangso.mhbackend.support.RedisTtlPolicy;
import com.hwangso.mhbackend.common.error.ApiException;          // [수정]
import com.hwangso.mhbackend.common.error.ErrorCode;            // [수정]
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReservationService {

    /**
     * 예약 수정(Update) + 예약 삭제(Delete) 공통 흐름
     * - 1) 현재 값 읽기, 예약 존재 여부 확인
     * - 2) bcrypt 비번 검증
     * - 3) 통과 시 Lua Script로 HSET Redis 변경
     */
    public void update(String date, String slot, String room, ReservationUpdateRequest req) {

        String rsvKey = RedisKeys.rsvKey(date, slot);

        Reservation current = repo.getReservation(rsvKey, room)
                .orElseThrow(() ->
                        new ApiException(ErrorCode.RESERVATION_NOT_FOUND)
                );

        if (!passwordEncoder.matches(req.password(), current.passwordHash())) {
            throw new ApiException(ErrorCode.PASSWORD_MISMATCH);
        }

        // null 또는 공백 필드는 기존 값 유지
        String newName =
                (req.name() == null || req.name().isBlank()) ? current.name() : req.name();
        String newCourse =
                (req.course() == null || req.course().isBlank()) ? current.course() : req.course();
        int newHeadcount =
                (req.headcount() == null) ? current.headcount() : req.headcount();

        Reservation updated =
                new Reservation(newName, newCourse, newHeadcount, current.passwordHash());

        String json = repo.toJson(updated);

        Long result = redis.execute(
                reservationUpdateScript,
                List.of(rsvKey),
                room,
                json
        );

        /**Lua 결과-> errorCode 매핑**/
        if (result == null) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
        if (result == 1L) return;
        if (result == -1L) {
            throw new ApiException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        throw new ApiException(ErrorCode.INTERNAL_ERROR);
    }

    private final StringRedisTemplate redis;
    private final ReservationRedisRepository repo;

    private final RedisScript<Long> reservationCreateScript;
    private final RedisScript<Long> reservationUpdateScript;
    private final RedisScript<Long> reservationDeleteScript;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 서버 시간대(아시아/서울)
    private final ZoneId zoneId = ZoneId.of("Asia/Seoul");

    public ReservationService(
            StringRedisTemplate redis,
            ReservationRedisRepository repo,
            RedisScript<Long> reservationCreateScript,
            RedisScript<Long> reservationUpdateScript,
            RedisScript<Long> reservationDeleteScript
    ) {
        this.redis = redis;
        this.repo = repo;
        this.reservationCreateScript = reservationCreateScript;
        this.reservationUpdateScript = reservationUpdateScript;
        this.reservationDeleteScript = reservationDeleteScript;
    }

    /**
     * 슬롯 전체 조회 (Read All)
     * <p>
     * - 이름 + 비밀번호가 모두 일치하는 예약만 반환
     * - 불일치는 에러가 아니라 "조회 조건 불충족"
     */
    public List<ReservationResponse> readAll(
            String date,
            String slot,
            String name,
            String password
    ) {
        String rsvKey = RedisKeys.rsvKey(date, slot);
        Map<Object, Object> entries = repo.getAll(rsvKey);

        return entries.entrySet().stream()
                .map(e -> {
                    Reservation r = repo.fromJson(String.valueOf(e.getValue()));

                    // 이름 불일치 → skip
                    if (!r.name().equals(name)) return null;

                    // 비밀번호 불일치 → skip
                    if (!passwordEncoder.matches(password, r.passwordHash())) return null;

                    // 이름 + 비밀번호 모두 일치
                    return new ReservationResponse(
                            date,
                            slot,
                            String.valueOf(e.getKey()),
                            new ReservationResponse.ReservationData(
                                    r.name(),
                                    r.course(),
                                    r.headcount()
                            )
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 예약 확정(Create)
     * 흐름:
     * 1. RedisKeys를 이용해 holdKey / rsvKey 생성
     * 2. date 기준 자정까지 남은 시간 계산 → TTL
     * 3. password bcrypt 해시
     * 4. Lua Script 실행 (원자 처리)
     * 5. Lua 반환값 → ErrorCode로 매핑
     */
    public void create(ReservationCreateRequest req) {

        String holdKey = RedisKeys.holdKey(req.date(), req.slot(), req.room());
        String rsvKey = RedisKeys.rsvKey(req.date(), req.slot());

        // date 기준 "자정까지 남은 초" → 당일 예약 자동 만료 정책
        long ttl = RedisTtlPolicy.untilMidnight(req.date(), zoneId);

        String passwordHash = passwordEncoder.encode(req.password());
        Reservation reservation =
                new Reservation(req.name(), req.course(), req.headcount(), passwordHash);

        String reservationJson = repo.toJson(reservation);

        Long result = redis.execute(
                reservationCreateScript,
                List.of(holdKey, rsvKey),
                req.holdToken(),
                req.room(),
                reservationJson,
                String.valueOf(ttl)
        );

        if (result == null) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }

        if (result == 1L) return;

        if (result == -1L) {
            throw new ApiException(ErrorCode.HOLD_NOT_FOUND);
        }
        if (result == 0L) {
            throw new ApiException(ErrorCode.HOLD_FORBIDDEN);
        }
        if (result == -2L) {
            throw new ApiException(ErrorCode.RESERVATION_CONFLICT);
        }

        throw new ApiException(ErrorCode.INTERNAL_ERROR);
    }

    /**
     * 단건 조회(Read One)
     * url date/slot/room을 알고 있어야,,
     * 이미 예약 구조를 알고 있는 사람(관리자)
     */
    public ReservationResponse readOne(String date, String slot, String room) {

        String rsvKey = RedisKeys.rsvKey(date, slot);

        Reservation r = repo.getReservation(rsvKey, room)
                .orElseThrow(() ->
                        new ApiException(ErrorCode.RESERVATION_NOT_FOUND)
                );

        return new ReservationResponse(
                date,
                slot,
                room,
                new ReservationResponse.ReservationData(
                        r.name(),
                        r.course(),
                        r.headcount()
                )
        );
    }

    /**
     * 예약 취소(Delete)
     */
    public void delete(String date, String slot, String room, String password) {

        String rsvKey = RedisKeys.rsvKey(date, slot);

        Reservation current = repo.getReservation(rsvKey, room)
                .orElseThrow(() ->
                        new ApiException(ErrorCode.RESERVATION_NOT_FOUND)
                );

        log.info("DELETE password raw = {}", password);
        log.info("DELETE password hash = {}", current.passwordHash());
        log.info(
                "PASSWORD MATCH = {}",
                passwordEncoder.matches(password, current.passwordHash())
        );

        if (!passwordEncoder.matches(password, current.passwordHash())) {
            throw new ApiException(ErrorCode.PASSWORD_MISMATCH);
        }

        Long result = redis.execute(
                reservationDeleteScript,
                List.of(rsvKey),
                room
        );

        if (result == null) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
        if (result == 1L) return;
        if (result == -1L) {
            throw new ApiException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        throw new ApiException(ErrorCode.INTERNAL_ERROR);
    }

    /**
     * 예약 시간/회의실 변경(Change)
     * - 기존 예약 1개 삭제
     * - 새 예약 1~2개 생성
     * - 비밀번호 기반 검증
     */
    public void change(ReservationChangeRequest req) {

        // 1. 기존 예약(from) 정보
        ReservationChangeRequest.FromReservation from = req.from();
        String fromRsvKey = RedisKeys.rsvKey(from.date(), from.slot());

        Reservation current = repo.getReservation(fromRsvKey, from.room())
                .orElseThrow(() ->
                        new ApiException(ErrorCode.RESERVATION_NOT_FOUND)
                );

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(req.password(), current.passwordHash())) {
            throw new ApiException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 3. 변경 대상(to) 충돌 검사
        for (ReservationChangeRequest.ToReservation to : req.to()) {
            String toRsvKey = RedisKeys.rsvKey(to.date(), to.slot());

            Optional<Reservation> exists =
                    repo.getReservation(toRsvKey, to.room());

            // 자기 자신으로의 변경은 허용
            boolean sameAsFrom =
                    from.date().equals(to.date()) &&
                            from.slot().equals(to.slot()) &&
                            from.room().equals(to.room());

            if (exists.isPresent() && !sameAsFrom) {
                throw new ApiException(ErrorCode.RESERVATION_CONFLICT);
            }
        }

        // 4. 기존 예약 삭제
        Long delResult = redis.execute(
                reservationDeleteScript,
                List.of(fromRsvKey),
                from.room()
        );

        if (delResult == null || delResult != 1L) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }

        // 5. 새 예약 생성 (1~2개)
        for (ReservationChangeRequest.ToReservation to : req.to()) {

            String toRsvKey = RedisKeys.rsvKey(to.date(), to.slot());

            long ttl = RedisTtlPolicy.untilMidnight(to.date(), zoneId);

            Reservation newReservation = new Reservation(
                    to.name(),
                    to.course(),
                    to.headcount(),
                    current.passwordHash() // 기존 비번 유지
            );

            String json = repo.toJson(newReservation);

            // hold 없이 직접 생성
            Boolean added = redis.opsForHash()
                    .putIfAbsent(toRsvKey, to.room(), json);

            if (Boolean.FALSE.equals(added)) {
                throw new ApiException(ErrorCode.RESERVATION_CONFLICT);
            }

            redis.expire(toRsvKey, ttl, java.util.concurrent.TimeUnit.SECONDS);
        }
    }


    /**
     * 관리자 권한 조회 로직
     * 1. 환경변수에서 admin.name, admin.password 읽음
     * 2. 환경변수에 있는 admin.name == 인자 name 그리고 admin.password == 인자 password 라면
     * 3. T 반환 그 외는 F 반환 -> 컨트롤러에서 T라면 "admin" 으로 프론트(사용자)에게 반환 !
     */
    @Value("${admin.name:}")
    private String adminName;

    @Value("${admin.password:}")
    private String adminPassword;

    public boolean isAdmin(String name, String password) {
        if (adminName == null || adminName.isBlank()) return false;
        if (adminPassword == null || adminPassword.isBlank()) return false;
        return adminName.equals(name) && adminPassword.equals(password);
    }

    /**
     * 관리자 전체 조회 (비밀번호 없이) (특정 날짜 + 방 고정) -> 예약 현황 한번에 조회
     * ex ) 2026-01-17,회의실 3 의 모든 날짜 조회
     */
    public List<ReservationResponse> adminReadRoomAllSlots(String date, String room) {
        List<String> slots = List.of(
                "part1", "part2", "part3", "part4", "part5", "part6", "part7", "part8"
        );

        List<ReservationResponse> out = new ArrayList<>();

        for (String slot : slots) {
            // slot 1개에 대한 전체(7개 room) 조회
            List<ReservationResponse> allRooms = adminReadAll(date, slot);

            // 그중에서 room만 골라서 있으면 추가
            allRooms.stream()
                    .filter(r -> room.equals(r.room()))   // ReservationResponse에 room getter/record 필드명에 맞춰 수정
                    .findFirst()
                    .ifPresent(out::add);
        }
        return out;
    }

    /**
     * 관리자 전체 조회 (비밀번호 없이) (특정 날짜 + 특정 시간대 고정) -> 그 시간대 모든 방 조회
     * ex ) 2026-01-17,09~10시 의 모든 예약한 방 조회
     */
    public List<ReservationResponse> adminReadAll(String date, String slot) {
        String rsvKey = RedisKeys.rsvKey(date, slot);
        Map<Object, Object> entries = repo.getAll(rsvKey);

        return entries.entrySet().stream()
                .map(e -> {
                    Reservation r = repo.fromJson(String.valueOf(e.getValue()));
                    return new ReservationResponse(
                            date,
                            slot,
                            String.valueOf(e.getKey()), // room
                            new ReservationResponse.ReservationData(
                                    r.name(),
                                    r.course(),
                                    r.headcount()
                            )
                    );
                })
                .toList();
    }

    /**
     * 관리자 강제 삭제(비번 없이)
     */
    public void adminForceDelete(String date, String slot, String room) {
        String rsvKey = RedisKeys.rsvKey(date, slot);

        Long result = redis.execute(
                reservationDeleteScript,
                List.of(rsvKey),
                room
        );

        if (result == null) throw new ApiException(ErrorCode.INTERNAL_ERROR);
        if (result == 1L) return;
        if (result == -1L) throw new ApiException(ErrorCode.RESERVATION_NOT_FOUND);

        throw new ApiException(ErrorCode.INTERNAL_ERROR);
    }
}