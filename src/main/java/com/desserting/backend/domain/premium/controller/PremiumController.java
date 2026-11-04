package com.desserting.backend.domain.premium.controller;

import com.desserting.backend.domain.premium.dto.PremiumStatusResponse;
import com.desserting.backend.domain.premium.dto.PremiumSubscribeRequest;
import com.desserting.backend.domain.premium.dto.PremiumSubscriptionResponse;
import com.desserting.backend.domain.premium.service.PremiumService;
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

@Tag(name = "프리미엄 패스 API", description = "PRE-01 프리미엄 패스 안내·모의 결제")
@RestController
@RequestMapping("/api/v1/premium")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PremiumController {

    private final PremiumService premiumService;

    @Operation(summary = "프리미엄 안내 및 내 이용 상태 (E-03)", description = "월 이용료, 혜택, 내 프리미엄 여부와 만료 시각을 반환합니다.")
    @GetMapping
    public ApiResponse<PremiumStatusResponse> getStatus(@Parameter(hidden = true) @CurrentUser Long userId) {
        return ApiResponse.success("프리미엄 이용 상태 조회에 성공하였습니다.", premiumService.getStatus(userId));
    }

    @Operation(summary = "프리미엄 패스 결제 (모의, PRE-01)",
            description = "월 9,900원을 모의 결제하고 30일간 프리미엄 회원이 됩니다. 이미 이용 중이면 C001.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "결제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001 이미 프리미엄 이용 중 / 결제 수단 누락",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/subscribe")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PremiumSubscriptionResponse> subscribe(@Parameter(hidden = true) @CurrentUser Long userId,
                                                              @Valid @RequestBody PremiumSubscribeRequest request) {
        return ApiResponse.success("프리미엄 패스 결제가 완료되었습니다.", premiumService.subscribe(userId, request));
    }

    @Operation(summary = "내 프리미엄 결제 내역")
    @GetMapping("/subscriptions")
    public ApiResponse<List<PremiumSubscriptionResponse>> getMySubscriptions(@Parameter(hidden = true) @CurrentUser Long userId) {
        return ApiResponse.success("프리미엄 결제 내역 조회에 성공하였습니다.", premiumService.getMySubscriptions(userId));
    }
}
