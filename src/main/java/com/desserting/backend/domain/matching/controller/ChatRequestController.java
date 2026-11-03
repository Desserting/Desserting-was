package com.desserting.backend.domain.matching.controller;

import com.desserting.backend.domain.matching.dto.ChatRequestAcceptResponse;
import com.desserting.backend.domain.matching.dto.ChatRequestCreateRequest;
import com.desserting.backend.domain.matching.dto.ChatRequestResponse;
import com.desserting.backend.domain.matching.service.ChatRequestService;
import com.desserting.backend.global.common.ApiResponse;
import com.desserting.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "03. Chat Request (하트 & 대화 요청)", description = "대화 요청 발송(하트 1개 차감), 받은 요청함 조회, 수락/거절 API")
@RestController
@RequestMapping("/api/v1/matches/requests")
@RequiredArgsConstructor
public class ChatRequestController {

    private final ChatRequestService chatRequestService;

    @Operation(summary = "대화 요청 전송 (REQ-02, HRT-02)",
            description = "상대방에게 대화 요청을 전송하고 하트 1개를 차감합니다 (프리미엄 회원은 차감 면제).")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatRequestResponse> sendRequest(
            @Parameter(hidden = true) @CurrentUser Long senderId,
            @Valid @RequestBody ChatRequestCreateRequest request) {
        ChatRequestResponse response = chatRequestService.sendRequest(senderId, request);
        return ApiResponse.success("대화 요청을 성공적으로 전송하였습니다.", response);
    }

    @Operation(summary = "받은 대화 요청 목록 조회 (REQ-01)",
            description = "내가 받은 대기 중(PENDING)인 대화 요청 목록을 조회합니다.")
    @GetMapping("/received")
    public ApiResponse<List<ChatRequestResponse>> getReceivedRequests(
            @Parameter(hidden = true) @CurrentUser Long receiverId) {
        List<ChatRequestResponse> responses = chatRequestService.getReceivedRequests(receiverId);
        return ApiResponse.success("받은 대화 요청 목록 조회에 성공하였습니다.", responses);
    }

    @Operation(summary = "대화 요청 수락 (REQ-01, CHT-01)",
            description = "수신한 대화 요청을 수락하고 1:1 실시간 채팅방을 즉시 개설합니다.")
    @PostMapping("/{requestId}/accept")
    public ApiResponse<ChatRequestAcceptResponse> acceptRequest(
            @Parameter(hidden = true) @CurrentUser Long currentUserId,
            @PathVariable Long requestId) {
        ChatRequestAcceptResponse response = chatRequestService.acceptRequest(currentUserId, requestId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "대화 요청 거절 (REQ-01, HRT-03)",
            description = "수신한 대화 요청을 거절하고, 보낸 사용자에게 하트 1개를 환불합니다.")
    @PostMapping("/{requestId}/reject")
    public ApiResponse<Void> rejectRequest(
            @Parameter(hidden = true) @CurrentUser Long currentUserId,
            @PathVariable Long requestId) {
        chatRequestService.rejectRequest(currentUserId, requestId);
        return ApiResponse.success("대화 요청을 거절하였습니다.", null);
    }
}
