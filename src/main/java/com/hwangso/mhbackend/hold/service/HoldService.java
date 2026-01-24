package com.hwangso.mhbackend.hold.service;

import com.hwangso.mhbackend.common.error.ApiException;
import com.hwangso.mhbackend.common.error.ErrorCode;
import com.hwangso.mhbackend.hold.dto.*;
import com.hwangso.mhbackend.hold.repository.HoldRedisRepository;
import com.hwangso.mhbackend.reservation.repository.ReservationRedisRepository;
import com.hwangso.mhbackend.support.RedisKeys;
import org.springframework.stereotype.Service;


import java.util.UUID;

@Service
public class HoldService {

    // 선점 TTL - 300초 (180~300 권장)
    private static final long HOLD_TTL_SECONDS = 300;

    private final HoldRedisRepository repo;
    private final ReservationRedisRepository reservationRepo;

    public HoldService(
            HoldRedisRepository repo,
            ReservationRedisRepository reservationRepo
    ) {
        this.repo = repo;
        this.reservationRepo = reservationRepo;
    }

    /**
     * acquire : 선점하기
     * @param  req ( date, slot, room )
     * @return HoldAcquireResponse( holdToken, expiresInSeconds )
     */
    public HoldAcquireResponse acquire(HoldAcquireRequest req) {

        // 1. 이미 예약 확정된 방인지 먼저 확인
        String rsvKey = RedisKeys.rsvKey(req.date(), req.slot());
        boolean alreadyReserved =
                reservationRepo.getReservation(rsvKey, req.room()).isPresent(); //isPresent() : 있으면 T/ 없다면 F/

        if (alreadyReserved) {
            throw new ApiException(ErrorCode.RESERVATION_CONFLICT);
        }

        // 2. 그 다음에 hold 선점
        String holdKey = RedisKeys.holdKey(req.date(), req.slot(), req.room());
        String token = UUID.randomUUID().toString();

        boolean ok = repo.acquireHold(holdKey, token, HOLD_TTL_SECONDS);
        if (!ok) throw new ApiException(ErrorCode.HOLD_CONFLICT);

        return new HoldAcquireResponse(token, HOLD_TTL_SECONDS);
    }

    /**
     * 토큰 상태 조회
     * @param req
     * @return HoldStatusResponse
     */
    public HoldStatusResponse status(HoldStatusRequest req) {
        String holdKey = RedisKeys.holdKey(req.date(), req.slot(), req.room());

        boolean held = repo.existsHold(holdKey);
        if (!held) return new HoldStatusResponse(false, 0);

        long ttl = repo.ttlSeconds(holdKey);

        // TTL 규칙 정리:
        // -2: 키 없음 (레이스 상황) -> held=false로 정리
        // -1: expire가 없다면(원래 hold는 EX 걸리므로 거의 없음) -> UI용으로 0 처리
        if (ttl <= 0) return new HoldStatusResponse(false, 0);

        return new HoldStatusResponse(true, ttl);
    }

    public void refresh(HoldRefreshRequest req) {
        String holdKey = RedisKeys.holdKey(req.date(), req.slot(), req.room());
        long res = repo.refreshHold(holdKey, req.holdToken(), HOLD_TTL_SECONDS);

        if (res == 1) return;
        if (res == -1) throw new ApiException(ErrorCode.HOLD_NOT_FOUND); // 404
        throw new ApiException(ErrorCode.HOLD_FORBIDDEN); // 403
    }

    public SlotRoomsStatusResponse roomsStatus(SlotRoomsStatusRequest req) {

        // 슬롯 하나에 대해 회의실 1~7번 상태를 담을 리스트
        var rooms = new java.util.ArrayList<RoomHoldStatus>(7);

        // 예약 확정 정보는 "slot 단위 Hash"에 저장되어 있으므로
        // 미리 rsvKey를 한 번만 만들어 둔다
        // (기존 코드는 holdKey만 사용했음)
        String rsvKey = RedisKeys.rsvKey(req.date(), req.slot());

        // 회의실 1번 ~ 7번 순회
        for (int r = 1; r <= 7; r++) {
            String room = String.valueOf(r);

            // 선점(hold) 상태는 room 단위 Key
            String holdKey = RedisKeys.holdKey(req.date(), req.slot(), room);

            // 현재 다른 사용자가 선점 중인지 확인
            boolean holdExists = repo.existsHold(holdKey);

            // 이미 예약이 "확정"되어 Redis Hash에 들어있는지 확인
            // → CRUD 추가 후 반드시 필요해진 검사
            boolean reservationExists =
                    reservationRepo.getReservation(rsvKey, room).isPresent();

            // "선점 중 OR 이미 예약됨" 이면 사용 불가
            boolean blocked = holdExists || reservationExists;

            // 예약도 안 됐고, 선점도 안 된 경우
            if (!blocked) {
                // false = 사용 가능
                // ttl = 0 (의미 없음)
                rooms.add(new RoomHoldStatus(room, false, 0));
                continue;
            }

            // 선점 중이면 TTL 의미 있음
            // 예약 확정 상태면 TTL 의미 없음 → 0
            long ttl = holdExists ? repo.ttlSeconds(holdKey) : 0;

            // true = 사용 불가(예약중)
            // ttl은 음수 방어 차원에서 0 이상으로 보정
            rooms.add(new RoomHoldStatus(room, true, Math.max(ttl, 0)));
        }

        // 슬롯 전체 회의실 상태 반환
        return new SlotRoomsStatusResponse(req.date(), req.slot(), rooms);
    }

    public void release(HoldReleaseRequest req) {
        String holdKey = RedisKeys.holdKey(req.date(), req.slot(), req.room());
        long res = repo.releaseHold(holdKey, req.holdToken());

        if (res == 1) return;
        if (res == -1) throw new ApiException(ErrorCode.HOLD_NOT_FOUND); // 404
        throw new ApiException(ErrorCode.HOLD_FORBIDDEN); // 403
    }

}