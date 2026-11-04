package com.desserting.backend.domain.order.controller;

import com.desserting.backend.domain.order.dto.OrderResponse;
import com.desserting.backend.domain.order.service.ReservationOrderService;
import com.desserting.backend.global.common.ApiResponse;
import com.desserting.backend.global.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 점주 화면용 주문 메뉴 내역. 준비 상태 변경은 기존 4-8 예약 상태 API(PATCH .../reservations/{id}/status)를 사용한다. */
@Tag(name = "4-7. 메뉴 주문 및 모의 결제 API")
@RestController
@RequestMapping("/api/v1/admin/stores/{storeId}/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminStoreOrderController {

    private final ReservationOrderService orderService;

    @Operation(summary = "매장 주문 메뉴 내역 (점주/관리자, ORD-01)",
            description = "매장에 들어온 선주문 메뉴·결제 금액을 최근순으로 조회합니다. 준비 상태는 reservationStatus 를 따릅니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "A002 점주·관리자 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "S001 매장 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ApiResponse<List<OrderResponse>> getStoreOrders(@PathVariable Long storeId) {
        return ApiResponse.success("매장 주문 내역 조회에 성공하였습니다.", orderService.getStoreOrders(storeId));
    }
}
