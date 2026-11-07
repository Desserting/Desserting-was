package com.desserting.backend.domain.admin.controller;

import com.desserting.backend.domain.admin.dto.AdminReservationResponse;
import com.desserting.backend.domain.admin.dto.ReservationStatusUpdateRequest;
import com.desserting.backend.domain.admin.service.AdminReservationService;
import com.desserting.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Reservations", description = "관리자 콘솔용 카페 방문 예약 및 주문 관리 API")
@RestController
@RequestMapping("/api/v1/admin/reservations")
@RequiredArgsConstructor
public class AdminReservationController {

    private final AdminReservationService adminReservationService;

    @Operation(summary = "전체 예약 및 주문 목록 조회 (관리자)", description = "전체 제휴 매장의 예약 접수 내역을 최신순으로 조회합니다.")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<List<AdminReservationResponse>>> getAllReservations() {
        List<AdminReservationResponse> response = adminReservationService.getAllReservations();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "예약 상태 변경 (관리자)", description = "예약 상태(CONFIRMED, PREPARING, COMPLETED, CANCELLED)를 변경합니다.")
    @PatchMapping("/{reservationId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<ApiResponse<AdminReservationResponse>> updateReservationStatus(
            @PathVariable Long reservationId,
            @Valid @RequestBody ReservationStatusUpdateRequest request) {
        AdminReservationResponse response = adminReservationService.updateReservationStatus(reservationId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("예약 상태가 변경되었습니다.", response));
    }
}
