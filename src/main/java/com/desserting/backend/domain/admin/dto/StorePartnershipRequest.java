package com.desserting.backend.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "매장 제휴 승인/반려 변경 요청 DTO")
public class StorePartnershipRequest {

    @Schema(description = "제휴 상태 (true: 승인 노출, false: 반려/노출 중단)", example = "true")
    @NotNull(message = "제휴 상태값은 필수입니다.")
    private Boolean isPartner;
}
