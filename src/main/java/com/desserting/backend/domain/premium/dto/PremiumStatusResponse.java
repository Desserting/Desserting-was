package com.desserting.backend.domain.premium.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "프리미엄 패스 안내 및 내 이용 상태 (E-03)")
public record PremiumStatusResponse(
        @Schema(description = "프리미엄 회원 여부", example = "false") boolean premium,
        @Schema(description = "이용 만료 시각 (결제 내역이 없거나 일반 회원이면 null)") LocalDateTime expiresAt,
        @Schema(description = "월 이용료", example = "9900") int monthlyPrice,
        @Schema(description = "이용 기간(일)", example = "30") int periodDays,
        @Schema(description = "혜택") List<String> benefits
) {
    public static final List<String> BENEFITS = List.of(
            "하트 차감 없이 대화 요청",
            "원하는 프로필에 직접 대화 요청",
            "나를 좋아요한 사람 확인",
            "이전 매칭 상대·프로필 방문자 확인");
}
