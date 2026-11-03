package com.desserting.backend.domain.heart.dto;

import com.desserting.backend.domain.heart.entity.HeartHistory;
import com.desserting.backend.domain.heart.entity.HeartHistoryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "하트 변동 내역 응답 DTO")
public class HeartHistoryResponse {

    @Schema(description = "변동 내역 ID", example = "1")
    private Long id;

    @Schema(description = "변동 수량 (+5, -1 등)", example = "-1")
    private Integer amount;

    @Schema(description = "변동 후 잔여 하트 수", example = "4")
    private Integer balanceAfter;

    @Schema(description = "변동 유형", example = "REQUEST_DEDUCT")
    private HeartHistoryType type;

    @Schema(description = "변동 사유", example = "대화 요청 전송 하트 1개 차감")
    private String description;

    @Schema(description = "발생 일시")
    private LocalDateTime createdAt;

    public static HeartHistoryResponse from(HeartHistory history) {
        return HeartHistoryResponse.builder()
                .id(history.getId())
                .amount(history.getAmount())
                .balanceAfter(history.getBalanceAfter())
                .type(history.getType())
                .description(history.getDescription())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
