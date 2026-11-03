package com.desserting.backend.domain.matching.controller;

import com.desserting.backend.domain.matching.dto.MatchCandidateResponse;
import com.desserting.backend.domain.matching.dto.RandomMatchResponse;
import com.desserting.backend.domain.matching.entity.MatchType;
import com.desserting.backend.domain.matching.service.MatchingService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "4-2. 랜덤 매칭 및 주변 탐색 API", description = "같은/다른 취향 랜덤 매칭 및 주변 사람 필터 API (4-2)")
@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MatchingController {

    private final MatchingService matchingService;

    @Operation(
            summary = "랜덤 매칭 상대 조회 (MAT-01~03, LOC-01~04)",
            description = """
                    취향 타입과 위치 조건에 맞는 상대 1명을 무작위로 반환합니다. 같은 조건으로 다시 호출하면 '다음 상대'입니다.
                    - radiusKm: 내 좌표(PATCH /users/me/location) 기준 반경. 좌표가 없으면 활동 지역으로 대체합니다.
                    - region: 지역명 접두어(예: 대구, 대구 남구). '전체' 또는 미입력 시 대구·경북 전체.
                    - 오늘 이미 본 상대와 차단 관계인 상대는 제외하며, 모두 소진하면 처음부터 다시 보여줍니다(recycled=true).
                    - 후보가 없으면 found=false 와 안내 문구(guide)를 반환합니다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공 (후보가 없어도 200, found=false)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "EX001 matchType 누락·잘못된 값 / EX101 반경 범위(1~100km) 초과",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "U001 사용자 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/random")
    public ApiResponse<RandomMatchResponse> findRandomMatch(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @Parameter(description = "매칭 타입", example = "SAME_TASTE") @RequestParam MatchType matchType,
            @Parameter(description = "반경(km, 1~100)", example = "10") @RequestParam(required = false) Double radiusKm,
            @Parameter(description = "지역명 접두어", example = "대구") @RequestParam(required = false) String region) {
        RandomMatchResponse response = matchingService.findRandomMatch(userId, matchType, radiusKm, region);
        return ApiResponse.success(response.isFound() ? "매칭 상대를 찾았습니다." : "조건에 맞는 상대가 없습니다.", response);
    }

    @Operation(summary = "이전 매칭 상대 목록 조회 (프리미엄 전용, MAT-04)",
            description = "랜덤 매칭에서 지나친(스킵한) 상대방 목록을 최근 순으로 조회합니다. 일반 회원은 403 차단됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "A002 프리미엄 전용",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/history")
    public ApiResponse<List<MatchCandidateResponse>> getSkippedMatchHistory(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @Parameter(description = "조회 개수 (기본 20, 최대 50)", example = "20") @RequestParam(defaultValue = "20") int limit) {
        List<MatchCandidateResponse> history = matchingService.getSkippedMatchHistory(userId, limit);
        return ApiResponse.success("이전 매칭 상대 이력 조회에 성공하였습니다.", history);
    }
}
