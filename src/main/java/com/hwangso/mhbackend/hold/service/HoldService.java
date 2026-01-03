package com.hwangso.mhbackend.hold.service;

import com.hwangso.mhbackend.common.error.ApiException;
import com.hwangso.mhbackend.common.error.ErrorCode;
import com.hwangso.mhbackend.hold.dto.*;
import com.hwangso.mhbackend.hold.repository.HoldRedisRepository;
import com.hwangso.mhbackend.support.RedisKeys;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class HoldService {

    // 선점 TTL - 300초 (180~300 권장)
    private static final long HOLD_TTL_SECONDS = 300;

    private final HoldRedisRepository repo;

    public HoldService(HoldRedisRepository repo) {
        this.repo = repo;
    }

    public HoldAcquireResponse acquire(HoldAcquireRequest req) {
        String holdKey = RedisKeys.holdKey(req.date(), req.slot(), req.room());
        String token = UUID.randomUUID().toString();

        boolean ok = repo.acquireHold(holdKey, token, HOLD_TTL_SECONDS);
        if (!ok) throw new ApiException(ErrorCode.HOLD_CONFLICT); // 409

        return new HoldAcquireResponse(token, HOLD_TTL_SECONDS);
    }

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
        var rooms = new java.util.ArrayList<RoomHoldStatus>(7);

        for (int r = 1; r <= 7; r++) {
            String room = String.valueOf(r);
            String holdKey = RedisKeys.holdKey(req.date(), req.slot(), room);

            boolean held = repo.existsHold(holdKey);
            if (!held) {
                rooms.add(new RoomHoldStatus(room, false, 0));
                continue;
            }
            long ttl = repo.ttlSeconds(holdKey);
            // 기존 status()와 동일 규칙
            if (ttl <= 0) {
                rooms.add(new RoomHoldStatus(room, false, 0));
            } else {
                rooms.add(new RoomHoldStatus(room, true, ttl));
            }
        }
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