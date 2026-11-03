package com.desserting.backend.domain.heart.controller;

import com.desserting.backend.domain.heart.dto.HeartPackageResponse;
import com.desserting.backend.domain.heart.dto.HeartPurchaseRequest;
import com.desserting.backend.domain.heart.dto.HeartPurchaseResponse;
import com.desserting.backend.domain.heart.service.HeartPurchaseService;
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

@Tag(name = "4-3. 하트 경제 및 충전 API", description = "하트 변동 내역 조회 및 잔여 하트 관리 API (4-3)")
@RestController
@RequestMapping("/api/v1/hearts")
@RequiredArgsConstructor
public class HeartPurchaseController {

    private final HeartPurchaseService heartPurchaseService;

    @Operation(summary = "하트 구매 상품 목록 (HRT-05)",
            description = "구매 가능한 하트 묶음과 가격을 반환합니다. 가격 = 하트 수 × 단가(현재 200원). 프론트는 이 값을 그대로 표시하면 됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/packages")
    public ApiResponse<List<HeartPackageResponse>> getPackages() {
        return ApiResponse.success("하트 상품 조회에 성공하였습니다.", heartPurchaseService.getPackages());
    }

    @Operation(summary = "하트 구매 - 모의 결제 (HRT-04, HRT-05)",
            description = "선택한 상품만큼 하트를 즉시 충전하고 하트 변동 내역(PURCHASE)에 기록합니다. 실제 결제는 일어나지 않습니다(모의 결제).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "구매 및 충전 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "C001 상품·결제수단 누락/잘못된 값, 프리미엄 회원(하트 무제한)은 구매 불가",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "U001 사용자 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HeartPurchaseResponse> purchase(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @Valid @RequestBody HeartPurchaseRequest request) {
        return ApiResponse.success("하트 구매가 완료되었습니다.", heartPurchaseService.purchase(userId, request));
    }

    @Operation(summary = "내 하트 구매 내역 (HRT-05)", description = "최근 구매순으로 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/purchases")
    public ApiResponse<List<HeartPurchaseResponse>> getMyPurchases(@Parameter(hidden = true) @CurrentUser Long userId) {
        return ApiResponse.success("하트 구매 내역 조회에 성공하였습니다.", heartPurchaseService.getMyPurchases(userId));
    }
}
