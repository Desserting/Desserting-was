package com.desserting.backend.domain.matching.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "대화 요청 전송 요청 DTO")
public class ChatRequestCreateRequest {

    @Schema(description = "대화 요청을 보낼 상대방 사용자 ID", example = "2")
    @NotNull(message = "상대방 사용자 ID는 필수입니다.")
    private Long receiverId;
}
