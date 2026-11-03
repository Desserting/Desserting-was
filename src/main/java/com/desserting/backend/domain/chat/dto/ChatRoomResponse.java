package com.desserting.backend.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "채팅방 목록 응답 DTO")
public class ChatRoomResponse {

    @Schema(description = "채팅방 고유 ID", example = "10")
    private Long roomId;

    @Schema(description = "채팅방 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String roomUuid;

    @Schema(description = "대화 상대방 사용자 ID", example = "2")
    private Long otherUserId;

    @Schema(description = "대화 상대방 닉네임", example = "초코크로플")
    private String otherUserNickname;

    @Schema(description = "대화 상대방 디저트 아바타 URL")
    private String otherUserAvatarUrl;

    @Schema(description = "최근 메시지 미리보기", example = "내일 앞산 카페거리에서 봬요!")
    private String lastMessagePreview;

    @Schema(description = "최근 메시지 일시")
    private LocalDateTime lastMessageAt;

    @Schema(description = "안 읽은 메시지 수", example = "2")
    @Builder.Default
    private Long unreadCount = 0L;
}
