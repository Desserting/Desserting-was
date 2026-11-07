package com.desserting.backend.domain.admin.controller;

import com.desserting.backend.domain.admin.dto.AdminReservationResponse;
import com.desserting.backend.domain.admin.dto.ReservationStatusUpdateRequest;
import com.desserting.backend.domain.admin.service.AdminOrderService;
import com.desserting.backend.domain.reservation.entity.ReservationStatus;
import com.desserting.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "4-8. 점주 실시간 예약 주문 관리 API", description = "점주의 실시간 예약 주문 확인 및 준비 상태 단계별 변경 (예약확정, 메뉴준비중, 방문완료, 취소) (4-8)")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @Operation(summary = "매장 예약/주문 목록 조회 (점주/관리자, ADM-02)", description = "매장에 접수된 데이트 예약 목록을 조회합니다. status 쿼리 파라미터로 필터링 가능합니다.")
    @GetMapping("/stores/{storeId}/reservations")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<List<AdminReservationResponse>>> getStoreReservations(
            @PathVariable Long storeId,
            @RequestParam(required = false) ReservationStatus status) {
        List<AdminReservationResponse> responses = adminOrderService.getStoreReservations(storeId, status);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "예약/주문 준비 상태 변경 (점주/관리자, ADM-02)",
            description = "상태를 단계적으로 변경합니다. (PENDING -> CONFIRMED -> PREPARING -> COMPLETED, 또는 CANCELLED)")
    @PatchMapping("/stores/{storeId}/reservations/{reservationId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<AdminReservationResponse>> updateReservationStatus(
            @PathVariable Long storeId,
            @PathVariable Long reservationId,
            @Valid @RequestBody ReservationStatusUpdateRequest request) {
        AdminReservationResponse response = adminOrderService.updateReservationStatus(storeId, reservationId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("예약 상태가 변경되었습니다.", response));
    }

    @Operation(summary = "전체 예약/주문 목록 조회 (플랫폼 관리자, ADM-02)", description = "플랫폼 전체 매장의 데이트 예약 목록을 조회합니다.")
    @GetMapping("/reservations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminReservationResponse>>> getAllReservations(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) ReservationStatus status) {
        List<AdminReservationResponse> responses = adminOrderService.getAllReservations(storeId, status);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "전체 예약 상태 변경 (플랫폼 관리자, ADM-02)")
    @PatchMapping("/reservations/{reservationId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminReservationResponse>> updateGlobalReservationStatus(
            @PathVariable Long reservationId,
            @Valid @RequestBody ReservationStatusUpdateRequest request) {
        AdminReservationResponse response = adminOrderService.updateGlobalReservationStatus(reservationId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("예약 상태가 변경되었습니다.", response));
    }
}
