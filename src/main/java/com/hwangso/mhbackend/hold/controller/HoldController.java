package com.hwangso.mhbackend.hold.controller;

import com.hwangso.mhbackend.hold.dto.*;
import com.hwangso.mhbackend.hold.service.HoldService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "1. Hold", description = "작성 중 선점(hold) 토큰 발급/조회/연장")
@RestController
@RequestMapping("/api/holds")
public class HoldController {

    private final HoldService service;

    public HoldController(HoldService service) {
        this.service = service;
    }

    /** 토큰 발급 + 선점 */
    @PostMapping
    public ResponseEntity<HoldAcquireResponse> acquire(@Valid @RequestBody HoldAcquireRequest req) {
        return ResponseEntity.ok(service.acquire(req));
    }

    /** 토큰 상태 조회*/
    @GetMapping("/status")
    public ResponseEntity<HoldStatusResponse> status(@Valid @ModelAttribute HoldStatusRequest req) {
        return ResponseEntity.ok(service.status(req));
    }

    /** 토큰 검증 후 TTL 연장 */
    @PatchMapping("/refresh")
    public ResponseEntity<Void> refresh(@Valid @RequestBody HoldRefreshRequest req) {
        service.refresh(req);
        return ResponseEntity.ok().build();
    }

    /** 선점 중 전체 조회 */
    @GetMapping("/rooms-status")
    public ResponseEntity<SlotRoomsStatusResponse> roomsStatus(@Valid @ModelAttribute SlotRoomsStatusRequest req) {
        return ResponseEntity.ok(service.roomsStatus(req));
    }

    /** 선점 취소 (TTL 만료전) */
    @DeleteMapping("/release")
    public ResponseEntity<Void> release(@Valid @RequestBody HoldReleaseRequest req) {
        service.release(req);
        return ResponseEntity.ok().build();
    }

}
