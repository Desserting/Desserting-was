package com.desserting.backend.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Schema(description = "데이트 매장 방문 제안 요청 DTO")
public class PlaceProposalRequest {

    @Schema(description = "제휴 매장 ID", example = "3")
    @NotNull(message = "매장 ID(storeId)는 필수입니다.")
    private Long storeId;

    @Schema(description = "방문 제안 메모 (선택)", example = "여기 분위기 너무 좋아 보여요! 이번 주말에 같이 가요.")
    private String message;
}
