package com.desserting.backend.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "관리자 하트 즉시 지급 요청 DTO")
public class HeartGrantRequest {

    @NotNull(message = "지급할 하트 개수는 필수입니다.")
    @Min(value = 1, message = "지급할 하트는 최소 1개 이상이어야 합니다.")
    @Schema(description = "지급할 하트 개수", example = "50")
    private Integer amount;

    @Schema(description = "지급 사유 (선택)", example = "심사용 테스트 재화 지급")
    private String reason;
}
