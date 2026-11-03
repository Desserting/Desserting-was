package com.desserting.backend.domain.matching.dto;

import com.desserting.backend.domain.matching.entity.ChatRequest;
import com.desserting.backend.domain.matching.entity.RequestStatus;
import com.desserting.backend.domain.user.entity.DessertType;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserDessertTag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "대화 요청 응답 DTO")
public class ChatRequestResponse {

    @Schema(description = "대화 요청 ID", example = "1")
    private Long requestId;

    @Schema(description = "요청을 보낸 사용자 ID", example = "2")
    private Long senderId;

    @Schema(description = "보낸 사람 디저트 닉네임", example = "딸기쇼트케이크")
    private String senderNickname;

    @Schema(description = "보낸 사람 디저트 아바타 URL")
    private String senderAvatarUrl;

    @Schema(description = "보낸 사람 나이", example = "25")
    private Integer senderAge;

    @Schema(description = "보낸 사람 활동 지역", example = "대구 남구")
    private String senderRegion;

    @Schema(description = "보낸 사람 선호 디저트 태그", example = "[\"CAKE\", \"COFFEE\"]")
    private List<DessertType> senderDessertTags;

    @Schema(description = "요청 처리 상태", example = "PENDING")
    private RequestStatus status;

    @Schema(description = "요청 발송 일시")
    private LocalDateTime createdAt;

    @Schema(description = "요청 만료 일시")
    private LocalDateTime expiredAt;

    public static ChatRequestResponse from(ChatRequest request) {
        User sender = request.getSender();
        List<DessertType> tags = sender.getDessertTags().stream()
                .map(UserDessertTag::getDessertType)
                .toList();

        return ChatRequestResponse.builder()
                .requestId(request.getId())
                .senderId(sender.getId())
                .senderNickname(sender.getNickname())
                .senderAvatarUrl(sender.getDessertAvatarUrl())
                .senderAge(sender.getAge())
                .senderRegion(sender.getRegion())
                .senderDessertTags(tags)
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .expiredAt(request.getExpiredAt())
                .build();
    }
}
