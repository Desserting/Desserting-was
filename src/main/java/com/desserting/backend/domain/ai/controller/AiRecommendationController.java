package com.desserting.backend.domain.ai.controller;

import com.desserting.backend.domain.ai.dto.AiRecommendationResponse;
import com.desserting.backend.domain.ai.service.AiRecommendationService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "4-4. 실시간 대화 및 채팅방 API", description = "참여 채팅방 목록, 1:1 대화방 개설, 메시지 이력 페이징, AI 데이트 추천 및 방 퇴장 API (4-4)")
@RestController
@RequestMapping("/api/v1/chats/rooms")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AiRecommendationController {

    private final AiRecommendationService aiRecommendationService;

    @Operation(summary = "채팅방 맞춤 제휴카페/메뉴 AI 추천 (P2, AI-01)",
            description = """
                    채팅방의 최근 대화 내역을 로컬 AI(Qwen 2.5 3B)가 분석하여 두 사람의 취향에 가장 어울리는 제휴 카페와 메뉴를 추천합니다.
                    추천된 결과는 채팅방에 'PLACE_PROPOSAL' 카드 메시지로 자동 발행되어 양측 사용자에게 실시간 노출됩니다.
                    Ollama 로컬 LLM이 미기동 상태이거나 지연될 경우 스마트 키워드 Fallback이 작동하여 안전하게 응답합니다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "AI 추천 성공 및 채팅방 카드 자동 발행 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "A001 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CH002 채팅방 참여 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "S001 추천 가능한 제휴 매장 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{roomId}/ai-recommendation")
    public ApiResponse<AiRecommendationResponse> getAiRecommendation(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @Parameter(description = "대화 중인 채팅방 ID", example = "10") @PathVariable Long roomId) {
        AiRecommendationResponse response = aiRecommendationService.recommendForRoom(roomId, userId);
        return ApiResponse.success("AI 맞춤 데이트 장소 추천이 완료되었습니다.", response);
    }
}
