package com.desserting.backend.domain.chat.dto;

import com.desserting.backend.domain.chat.document.ChatMessageDocument;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "채팅 메시지 DTO")
public class ChatMessageDto implements Serializable {

    @Schema(description = "메시지 고유 ID (MongoDB ObjectId)", example = "654321abcdef...")
    private String messageId;

    @Schema(description = "채팅방 ID (PostgreSQL PK)", example = "10")
    private Long roomId;

    @Schema(description = "보낸 사용자 ID", example = "1")
    private Long senderId;

    @Schema(description = "보낸 사용자 닉네임", example = "딸기쇼트케이크")
    private String senderNickname;

    @Schema(description = "보낸 사용자 아바타 URL")
    private String senderAvatarUrl;

    @Schema(description = "메시지 타입 (TEXT, IMAGE, PLACE_PROPOSAL, SYSTEM)", example = "TEXT")
    private String messageType;

    @Schema(description = "메시지 내용", example = "앞산 카페거리 르페리고에서 내일 3시 어때요?")
    private String content;

    @Schema(description = "데이트 매장 제안 메타데이터 (가게 ID, 이름, 제안 일시 등)")
    private Map<String, Object> metadata;

    @Schema(description = "메시지 생성 일시")
    private LocalDateTime createdAt;

    public static ChatMessageDto from(ChatMessageDocument doc, String avatarUrl) {
        return ChatMessageDto.builder()
                .messageId(doc.getId())
                .roomId(doc.getRoomId())
                .senderId(doc.getSenderId())
                .senderNickname(doc.getSenderNickname())
                .senderAvatarUrl(avatarUrl)
                .messageType(doc.getMessageType())
                .content(doc.getContent())
                .metadata(doc.getMetadata())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
