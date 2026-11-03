package com.desserting.backend.domain.heart.controller;

import com.desserting.backend.domain.heart.dto.HeartHistoryResponse;
import com.desserting.backend.domain.heart.service.HeartService;
import com.desserting.backend.global.common.ApiResponse;
import com.desserting.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "4-3. 하트 경제 및 충전 API", description = "하트 변동 내역 조회 및 잔여 하트 관리 API (4-3)")
@RestController
@RequestMapping("/api/v1/hearts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class HeartController {

    private final HeartService heartService;

    @Operation(summary = "내 하트 변동 내역 및 잔여량 조회 (HRT-01)", description = "로그인된 사용자의 하트 충전, 대화 요청 차감, 환불 등 변동 이력을 최신순으로 조회합니다.")
    @GetMapping("/history")
    public ApiResponse<List<HeartHistoryResponse>> getMyHeartHistories(
            @Parameter(hidden = true) @CurrentUser Long userId) {
        List<HeartHistoryResponse> histories = heartService.getMyHeartHistories(userId);
        return ApiResponse.success("하트 변동 내역 조회에 성공하였습니다.", histories);
    }
}
