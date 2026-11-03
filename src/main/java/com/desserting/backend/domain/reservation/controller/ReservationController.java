package com.desserting.backend.domain.reservation.controller;

import com.desserting.backend.domain.reservation.dto.CreateReservationRequest;
import com.desserting.backend.domain.reservation.dto.ReservationDateResponse;
import com.desserting.backend.domain.reservation.dto.ReservationResponse;
import com.desserting.backend.domain.reservation.dto.ReservationSlotResponse;
import com.desserting.backend.domain.reservation.service.ReservationService;
import com.desserting.backend.global.common.ApiResponse;
import com.desserting.backend.global.error.ErrorResponse;
import com.desserting.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "4-6. 매장 테이블 예약 API", description = "예약 가능 날짜·시간 조회, 예약 생성·조회·취소 API (4-6)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "예약 가능 날짜 (RSV-01)", description = "기본 오늘부터 14일, 최대 31일. 슬롯이 있는 날짜만 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "EX301 조회 기간 역전·31일 초과 / EX001 날짜 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "S001 매장 없음 또는 제휴 해지",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/stores/{storeId}/reservation-dates")
    public ApiResponse<List<ReservationDateResponse>> getAvailableDates(
            @PathVariable Long storeId,
            @Parameter(example = "2026-11-03") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(example = "2026-11-16") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success("예약 가능 날짜 조회에 성공하였습니다.",
                reservationService.getAvailableDates(storeId, from, to));
    }

    @Operation(summary = "예약 가능 시간대 (RSV-02)", description = "해당 날짜의 시간대별 정원·잔여 팀 수. 예약 1건이 1팀을 차지합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공 (슬롯이 없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "EX001 date 누락·형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "S001 매장 없음 또는 제휴 해지",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/stores/{storeId}/reservation-slots")
    public ApiResponse<List<ReservationSlotResponse>> getSlots(
            @PathVariable Long storeId,
            @Parameter(example = "2026-11-15") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success("예약 시간대 조회에 성공하였습니다.",
                reservationService.getSlots(storeId, date));
    }

    @Operation(summary = "예약 생성 (RSV-03)", description = "선택한 시간대의 정원 1팀을 차감하고 PENDING(예약 대기) 상태로 예약을 만듭니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "예약 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "C001 요청 값 검증 실패(인원 1~10명 등) / EX001 JSON 형식 오류 / EX302 이미 지난 시간대",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "R003 슬롯 없음 / S001 매장 없음 또는 제휴 해지 / U001 사용자 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "R001 정원 마감·판매중지 / EX303 같은 시간대 중복 예약",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReservationResponse> createReservation(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @Valid @RequestBody CreateReservationRequest request) {
        return ApiResponse.success("예약이 생성되었습니다.", reservationService.createReservation(userId, request));
    }

    @Operation(summary = "내 예약 내역 (RSV-03)", description = "최근 예약 날짜순으로 반환합니다 (취소 건 포함).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/reservations/me")
    public ApiResponse<List<ReservationResponse>> getMyReservations(@Parameter(hidden = true) @CurrentUser Long userId) {
        return ApiResponse.success("예약 내역 조회에 성공하였습니다.", reservationService.getMyReservations(userId));
    }

    @Operation(summary = "예약 상세 조회 (RSV-03)", description = "본인 예약만 조회할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "R002 예약 없음 또는 본인 예약 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/reservations/{reservationId}")
    public ApiResponse<ReservationResponse> getMyReservation(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long reservationId) {
        return ApiResponse.success("예약 조회에 성공하였습니다.", reservationService.getMyReservation(userId, reservationId));
    }

    @Operation(summary = "예약 취소 (RSV-04)", description = "PENDING/CONFIRMED 예약만 취소할 수 있으며 시간대 자리가 반환됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "EX304 준비 중·완료·이미 취소된 예약",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "R002 예약 없음 또는 본인 예약 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/reservations/{reservationId}/cancel")
    public ApiResponse<ReservationResponse> cancelReservation(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long reservationId) {
        return ApiResponse.success("예약이 취소되었습니다.", reservationService.cancelReservation(userId, reservationId));
    }
}
