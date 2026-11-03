package com.desserting.backend.domain.chat.controller;

import com.desserting.backend.domain.chat.dto.ChatMessageDto;
import com.desserting.backend.domain.chat.dto.ChatRoomResponse;
import com.desserting.backend.domain.chat.service.ChatService;
import com.desserting.backend.global.common.ApiResponse;
import com.desserting.backend.global.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "04. Real-time Chat (실시간 채팅)", description = "채팅방 목록 조회, 이전 메시지 커서 페이징 조회 및 퇴장 API")
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;

    @Operation(summary = "내 활성 채팅방 목록 조회 (CHT-01)",
            description = "로그인된 사용자가 참여 중인 활성 1:1 채팅방 목록과 상대방 정보, 최근 메시지를 조회합니다.")
    @GetMapping("/rooms")
    public ApiResponse<List<ChatRoomResponse>> getMyChatRooms(
            @Parameter(hidden = true) @CurrentUser Long userId) {
        List<ChatRoomResponse> responses = chatService.getMyChatRooms(userId);
        return ApiResponse.success("채팅방 목록 조회에 성공하였습니다.", responses);
    }

    @Operation(summary = "채팅방 메시지 이력 페이징 조회 (CHT-02)",
            description = "MongoDB에 저장된 채팅 메시지를 커서 기반(무한 스크롤)으로 최신순 페이징 조회합니다.")
    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<List<ChatMessageDto>> getChatMessages(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long roomId,
            @Parameter(description = "이전 페이지의 마지막 메시지 ID (첫 요청 시 생략)") @RequestParam(required = false) String cursorId,
            @Parameter(description = "가져올 메시지 개수 (기본 30개)") @RequestParam(defaultValue = "30") int size) {
        List<ChatMessageDto> messages = chatService.getChatMessages(userId, roomId, cursorId, size);
        return ApiResponse.success("채팅 메시지 내역 조회에 성공하였습니다.", messages);
    }

    @Operation(summary = "채팅방 나가기", description = "해당 채팅방에서 퇴장(Soft Delete)합니다.")
    @PostMapping("/rooms/{roomId}/leave")
    public ApiResponse<Void> leaveChatRoom(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long roomId) {
        chatService.leaveChatRoom(userId, roomId);
        return ApiResponse.success("채팅방에서 정상적으로 퇴장하였습니다.", null);
    }
}
