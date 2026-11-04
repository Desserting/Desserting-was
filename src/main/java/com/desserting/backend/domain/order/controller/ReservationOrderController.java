package com.desserting.backend.domain.order.controller;

import com.desserting.backend.domain.order.dto.CreateOrderRequest;
import com.desserting.backend.domain.order.dto.OrderPreviewRequest;
import com.desserting.backend.domain.order.dto.OrderPreviewResponse;
import com.desserting.backend.domain.order.dto.OrderResponse;
import com.desserting.backend.domain.order.service.ReservationOrderService;
import com.desserting.backend.global.common.ApiResponse;
import com.desserting.backend.global.error.ErrorResponse;
import com.desserting.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "4-7. 메뉴 주문 및 모의 결제 API", description = "예약 메뉴 선주문, 쿠폰 할인, 합산·개별 모의 결제 (4-7)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReservationOrderController {

    private final ReservationOrderService orderService;

    @Operation(summary = "주문 금액 미리보기 (ORD-01, 7-1 주문 확인)",
            description = "메뉴 합계·쿠폰 할인·실결제 금액·1인당 금액을 계산만 하고 저장하지 않습니다. couponId 생략 시 예약할 때 고른 쿠폰을 사용합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "계산 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "C001 요청 값 검증 실패 / EX001 JSON 형식 오류 / EX305 쿠폰 사용 불가(최소 주문 금액·만료·중지·다른 매장) / EX401 주문 불가 예약 상태 / EX403 다른 매장 메뉴",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "R002 예약 없음 또는 본인 예약 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "EX404 품절 메뉴 포함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/reservations/{reservationId}/order/preview")
    public ApiResponse<OrderPreviewResponse> preview(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long reservationId,
            @Valid @RequestBody OrderPreviewRequest request) {
        return ApiResponse.success("주문 금액을 계산하였습니다.", orderService.preview(userId, reservationId, request));
    }

    @Operation(summary = "메뉴 주문 + 모의 결제 (ORD-01, 7-2 결제)",
            description = "결제 승인(모의)과 동시에 주문을 만들고, 예약의 totalAmount/discountAmount/finalAmount 를 갱신합니다. "
                    + "예약 1건당 1번만 주문할 수 있습니다. SPLIT 은 예약 인원(guestCount)으로 나눕니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "결제 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "C001 요청 값 검증 실패 / EX001 JSON 형식 오류 / EX305 쿠폰 사용 불가 / EX401 주문 불가 예약 상태 / EX403 다른 매장 메뉴 / EX406 1인 예약 개별 결제",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "R002 예약 없음 또는 본인 예약 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "EX402 이미 결제된 예약 / EX404 품절 메뉴 포함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/reservations/{reservationId}/order")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> placeOrder(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long reservationId,
            @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success("주문 및 모의 결제가 완료되었습니다.", orderService.placeOrder(userId, reservationId, request));
    }

    @Operation(summary = "예약의 주문 영수증 조회 (ORD-01)", description = "본인 예약의 주문만 조회할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "EX405 주문 없음 또는 본인 주문 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/reservations/{reservationId}/order")
    public ApiResponse<OrderResponse> getOrder(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long reservationId) {
        return ApiResponse.success("주문 조회에 성공하였습니다.", orderService.getOrderOfReservation(userId, reservationId));
    }

    @Operation(summary = "내 주문 내역 (ORD-01)", description = "최근 주문순 (환불 건 포함)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/orders/me")
    public ApiResponse<List<OrderResponse>> getMyOrders(@Parameter(hidden = true) @CurrentUser Long userId) {
        return ApiResponse.success("주문 내역 조회에 성공하였습니다.", orderService.getMyOrders(userId));
    }
}
