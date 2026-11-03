package com.desserting.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "위치 정보 업데이트 요청 DTO")
public class UpdateLocationRequest {

    @Schema(description = "위도 (GPS 허용 시)", example = "35.839812")
    @DecimalMin(value = "33.0", message = "올바른 대한민국 위도 범위를 입력해주세요.")
    @DecimalMax(value = "39.0", message = "올바른 대한민국 위도 범위를 입력해주세요.")
    private BigDecimal latitude;

    @Schema(description = "경도 (GPS 허용 시)", example = "128.587632")
    @DecimalMin(value = "124.0", message = "올바른 대한민국 경도 범위를 입력해주세요.")
    @DecimalMax(value = "132.0", message = "올바른 대한민국 경도 범위를 입력해주세요.")
    private BigDecimal longitude;

    @Schema(description = "수동 선택 지역 (GPS 비허용 또는 보조용)", example = "대구 남구")
    private String region;
}
