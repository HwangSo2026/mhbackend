package com.hwangso.mhbackend.reservation.controller;

import com.hwangso.mhbackend.reservation.dto.*;
import com.hwangso.mhbackend.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "2. Reservation", description = "예약 확정/조회/수정/취소")
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    /** 예약 확정(Create) */
    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody ReservationCreateRequest req) {
        service.create(req);
        return ResponseEntity.ok().build();
    }

    /** 슬롯 전체 조회(Read All) */
    @GetMapping
    public ResponseEntity<List<ReservationResponse>> readAll(
            @RequestParam String date,
            @RequestParam String slot,
            @RequestParam String name,
            @RequestParam String password
    ) {
        return ResponseEntity.ok(
                service.readAll(date, slot, name, password)
        );
    }

    /** 단건 조회(Read One) */
    @GetMapping("/{date}/{slot}/{room}")
    public ResponseEntity<ReservationResponse> readOne(
            @PathVariable @Pattern(regexp="^\\d{4}-\\d{2}-\\d{2}$") String date,
            @PathVariable @Pattern(regexp="^part[1-8]$") String slot,
            @PathVariable @Pattern(regexp="^[1-7]$") String room
    ) {
        return ResponseEntity.ok(service.readOne(date, slot, room));
    }

    /** 예약 수정(Update) */
    @PutMapping("/{date}/{slot}/{room}")
    public ResponseEntity<Void> update(
            @PathVariable @Pattern(regexp="^\\d{4}-\\d{2}-\\d{2}$") String date,
            @PathVariable @Pattern(regexp="^part[1-8]$") String slot,
            @PathVariable @Pattern(regexp="^[1-7]$") String room,
            @Valid @RequestBody ReservationUpdateRequest req
    ) {
        service.update(date, slot, room, req);
        return ResponseEntity.ok().build();
    }

    /** 예약 취소(Delete) */
    @DeleteMapping("/{date}/{slot}/{room}")
    public ResponseEntity<Void> delete(
            @PathVariable String date,
            @PathVariable String slot,
            @PathVariable String room,
            @Valid @RequestBody ReservaionDeleteRequest.ReservationDeleteRequest req
    ) {
        service.delete(date, slot, room, req.password());
        return ResponseEntity.ok().build();
    }
    /** 예약 시간/회의실 변경(Change) */
    @PostMapping("/change")
    public ResponseEntity<Void> change(
            @Valid @RequestBody ReservationChangeRequest req
    ) {
        service.change(req);
        return ResponseEntity.ok().build();
    }
}