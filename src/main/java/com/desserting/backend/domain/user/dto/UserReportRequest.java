package com.desserting.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "사용자 신고 요청 DTO")
public class UserReportRequest {

    @Schema(description = "신고 사유 카테고리 (SPAM, ABUSE, HARASSMENT, FRAUD, INAPPROPRIATE_PROFILE, OTHER)", example = "SPAM")
    @NotBlank(message = "신고 사유는 필수입니다.")
    private String reason;

    @Schema(description = "상세 신고 내용", example = "상업적 광고 메시지를 지속적으로 발송합니다.")
    private String description;
}
