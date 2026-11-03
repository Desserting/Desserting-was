package com.desserting.backend.domain.user.controller;

import com.desserting.backend.domain.user.dto.OtherUserProfileResponse;
import com.desserting.backend.domain.user.dto.UserReportRequest;
import com.desserting.backend.domain.user.service.SocialService;
import com.desserting.backend.global.common.ApiResponse;
import com.desserting.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "05. Social & Safety (좋아요 & 차단/신고)", description = "상대방 좋아요(LIKE), 나를 좋아요한 사람 목록(프리미엄), 차단 및 신고 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;

    @Operation(summary = "상대방 프로필 좋아요 토글 (LIKE-01)", description = "상대방 프로필에 좋아요를 누르거나 취소합니다.")
    @PostMapping("/{userId}/like")
    public ApiResponse<Map<String, Boolean>> toggleLike(
            @Parameter(hidden = true) @CurrentUser Long currentUserId,
            @PathVariable Long userId) {
        boolean liked = socialService.toggleLike(currentUserId, userId);
        return ApiResponse.success(liked ? "좋아요를 눌렀습니다." : "좋아요를 취소했습니다.", Map.of("liked", liked));
    }

    @Operation(summary = "나를 좋아요한 사람 목록 조회 (LIKE-02)", description = "프리미엄 패스 회원 전용 기능으로, 나에게 좋아요를 보낸 사용자 목록을 조회합니다.")
    @GetMapping("/me/likes")
    public ApiResponse<List<OtherUserProfileResponse>> getMyLikesReceived(
            @Parameter(hidden = true) @CurrentUser Long currentUserId) {
        List<OtherUserProfileResponse> responses = socialService.getMyLikesReceived(currentUserId);
        return ApiResponse.success("나를 좋아요한 사람 목록 조회에 성공하였습니다.", responses);
    }

    @Operation(summary = "사용자 차단 (SEC-01)", description = "특정 사용자를 차단합니다.")
    @PostMapping("/{userId}/block")
    public ApiResponse<Void> blockUser(
            @Parameter(hidden = true) @CurrentUser Long currentUserId,
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "사용자 직접 차단") String reason) {
        socialService.blockUser(currentUserId, userId, reason);
        return ApiResponse.success("해당 사용자를 차단하였습니다.", null);
    }

    @Operation(summary = "사용자 신고 (SEC-01)", description = "특정 사용자의 부적절한 행위를 사유와 함께 신고합니다.")
    @PostMapping("/{userId}/report")
    public ApiResponse<Void> reportUser(
            @Parameter(hidden = true) @CurrentUser Long currentUserId,
            @PathVariable Long userId,
            @Valid @RequestBody UserReportRequest request) {
        socialService.reportUser(currentUserId, userId, request.getReason(), request.getDescription());
        return ApiResponse.success("신고가 성공적으로 접수되었습니다.", null);
    }
}
