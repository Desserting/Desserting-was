package com.desserting.backend.domain.reservation.controller;

import com.desserting.backend.domain.reservation.dto.CreateReservationRequest;
import com.desserting.backend.domain.reservation.dto.ReservationDateResponse;
import com.desserting.backend.domain.reservation.dto.ReservationResponse;
import com.desserting.backend.domain.reservation.dto.ReservationSlotResponse;
import com.desserting.backend.domain.reservation.service.ReservationService;
import com.desserting.backend.global.common.ApiResponse;
import com.desserting.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "07. Reservation (매장 예약)", description = "예약 가능 날짜·시간 조회, 예약 생성·조회·취소 API (4-6)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "예약 가능 날짜 (RSV-01)", description = "기본 오늘부터 14일, 최대 31일. 슬롯이 있는 날짜만 반환합니다.")
    @GetMapping("/stores/{storeId}/reservation-dates")
    public ApiResponse<List<ReservationDateResponse>> getAvailableDates(
            @PathVariable Long storeId,
            @Parameter(example = "2026-11-03") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(example = "2026-11-16") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success("예약 가능 날짜 조회에 성공하였습니다.",
                reservationService.getAvailableDates(storeId, from, to));
    }

    @Operation(summary = "예약 가능 시간대 (RSV-02)", description = "해당 날짜의 시간대별 정원·잔여 팀 수. 예약 1건이 1팀을 차지합니다.")
    @GetMapping("/stores/{storeId}/reservation-slots")
    public ApiResponse<List<ReservationSlotResponse>> getSlots(
            @PathVariable Long storeId,
            @Parameter(example = "2026-11-15") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success("예약 시간대 조회에 성공하였습니다.",
                reservationService.getSlots(storeId, date));
    }

    @Operation(summary = "예약 생성 (RSV-03)", description = "선택한 시간대의 정원 1팀을 차감하고 PENDING(예약 대기) 상태로 예약을 만듭니다.")
    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReservationResponse> createReservation(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @Valid @RequestBody CreateReservationRequest request) {
        return ApiResponse.success("예약이 생성되었습니다.", reservationService.createReservation(userId, request));
    }

    @Operation(summary = "내 예약 내역 (RSV-03)")
    @GetMapping("/reservations/me")
    public ApiResponse<List<ReservationResponse>> getMyReservations(@Parameter(hidden = true) @CurrentUser Long userId) {
        return ApiResponse.success("예약 내역 조회에 성공하였습니다.", reservationService.getMyReservations(userId));
    }

    @Operation(summary = "예약 상세")
    @GetMapping("/reservations/{reservationId}")
    public ApiResponse<ReservationResponse> getMyReservation(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long reservationId) {
        return ApiResponse.success("예약 조회에 성공하였습니다.", reservationService.getMyReservation(userId, reservationId));
    }

    @Operation(summary = "예약 취소 (RSV-04)", description = "PENDING/CONFIRMED 예약만 취소할 수 있으며 시간대 자리가 반환됩니다.")
    @PatchMapping("/reservations/{reservationId}/cancel")
    public ApiResponse<ReservationResponse> cancelReservation(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long reservationId) {
        return ApiResponse.success("예약이 취소되었습니다.", reservationService.cancelReservation(userId, reservationId));
    }
}
