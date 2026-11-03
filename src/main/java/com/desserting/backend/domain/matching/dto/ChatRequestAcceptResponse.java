package com.desserting.backend.domain.matching.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "대화 요청 수락 결과 응답 DTO")
public class ChatRequestAcceptResponse {

    @Schema(description = "처리된 요청 ID", example = "1")
    private Long requestId;

    @Schema(description = "생성된 채팅방 고유 ID (PK)", example = "10")
    private Long chatRoomId;

    @Schema(description = "채팅방 고유 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String roomUuid;

    @Schema(description = "안내 메시지", example = "대화 요청이 수락되어 채팅방이 개설되었습니다.")
    private String message;
}
