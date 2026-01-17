package com.hwangso.mhbackend.reservation.admin;

import com.hwangso.mhbackend.reservation.dto.ReservationResponse;
import com.hwangso.mhbackend.reservation.service.ReservationService;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ReservationService reservationService;
    private final AdminService tokenService;

    /**
     * 관리자 검증 API
     *
     * @param req { name, pasword } : RequestBody 로 이름, 비밀번호 받아와서 검증
     * @return role, token : 성공시 "admin", 관리자 토큰 반환
     */
    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@RequestBody AdminLoginRequest req) {
        if (!reservationService.isAdmin(req.name(), req.password())) {
            return ResponseEntity.status(401).build();
        }
        String token = tokenService.issueToken();
        return ResponseEntity.ok(new AdminLoginResponse("admin", token));
    }

    /**
     * 관리자 전체 조회 API
     *
     * @param token : 관리자 토큰
     * @param date  : 날짜
     * @param slot  : 회의실
     * @return List<ReservationResponse> 전체 예약 리스트 반환
     */
    @GetMapping("/reservations")
    public ResponseEntity<List<ReservationResponse>> readAll(
            @RequestHeader("X-Admin-Token") String token,
            @RequestParam String date,
            @RequestParam String slot
    ) {
        if (!tokenService.isValid(token)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(reservationService.adminReadAll(date, slot));
    }

    /**
     * 관리자 강제 취소 API (1개씩)
     *
     * @param token : 관리자 토큰
     * @param date
     * @param slot
     * @param room
     * @return Void : 없음
     */
    @DeleteMapping("/reservations/{date}/{slot}/{room}")
    public ResponseEntity<Void> forceDelete(
            @RequestHeader("X-Admin-Token") String token,
            @PathVariable String date,
            @PathVariable String slot,
            @PathVariable String room
    ) {
        if (!tokenService.isValid(token)) return ResponseEntity.status(401).build();
        reservationService.adminForceDelete(date, slot, room);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/rooms/{date}/{room}")
    public ResponseEntity<List<ReservationResponse>> readRoomAllSlots(
            @RequestHeader("X-Admin-Token") String token,
            @PathVariable String date,
            @PathVariable String room
    ) {
        if(!tokenService.isValid(token)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(reservationService.adminReadRoomAllSlots(date, room));
    }

}