package com.desserting.backend.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "채팅 메시지 전송 요청 DTO")
public class ChatMessageRequest {

    @Schema(description = "채팅방 ID (PostgreSQL PK)", example = "10")
    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long roomId;

    @Schema(description = "메시지 타입 (TEXT, IMAGE, PLACE_PROPOSAL, SYSTEM)", example = "TEXT")
    @NotBlank(message = "메시지 타입은 필수입니다.")
    @Builder.Default
    private String messageType = "TEXT";

    @Schema(description = "메시지 내용", example = "안녕하세요! 반갑습니다.")
    @NotBlank(message = "메시지 내용은 비어있을 수 없습니다.")
    private String content;

    @Schema(description = "가게 방문 제안 시 매장 정보 및 일시 메타데이터")
    private Map<String, Object> metadata;
}
