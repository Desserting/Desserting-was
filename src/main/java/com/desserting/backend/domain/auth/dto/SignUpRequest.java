package com.desserting.backend.domain.auth.dto;

import com.desserting.backend.domain.user.entity.DessertType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "회원가입 요청 DTO")
public class SignUpRequest {

    @Schema(description = "이메일", example = "user@desserting.com")
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @Schema(description = "비밀번호 (6자 이상)", example = "password123!")
    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Size(min = 6, max = 50, message = "비밀번호는 6자 이상 50자 이하이어야 합니다.")
    private String password;

    @Schema(description = "디저트 닉네임", example = "딸기쇼트케이크")
    @NotBlank(message = "닉네임은 필수 입력값입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하이어야 합니다.")
    private String nickname;

    @Schema(description = "성별 (MALE / FEMALE)", example = "FEMALE")
    @NotBlank(message = "성별은 필수 선택값입니다.")
    @Pattern(regexp = "^(MALE|FEMALE)$", message = "성별은 MALE 또는 FEMALE이어야 합니다.")
    private String gender;

    @Schema(description = "나이 (성인 기준 만 19세 이상 필수)", example = "25")
    @NotNull(message = "나이는 필수 입력값입니다.")
    @Min(value = 19, message = "본 서비스는 만 19세 이상의 성인만 이용 가능합니다.")
    @Max(value = 99, message = "올바른 나이를 입력해주세요.")
    private Integer age;

    @Schema(description = "활동 지역 (대구/경북 구·군)", example = "대구 남구")
    @NotBlank(message = "활동 지역은 필수 입력값입니다.")
    private String region;

    @Schema(description = "선호 디저트 취향 목록 (최소 1개 이상 선택)", example = "[\"CAKE\", \"COFFEE\"]")
    @NotEmpty(message = "디저트 취향은 최소 1개 이상 선택해야 합니다.")
    private List<DessertType> dessertTypes;

    @Schema(description = "대표 아바타 URL (선택). GET /api/v1/users/avatars 목록의 avatarUrl만 허용. 생략 시 첫 번째 디저트 취향의 기본 아바타로 자동 지정",
            example = "https://desserting-web.vercel.app/avatars/cake.png")
    private String dessertAvatarUrl;

    @Schema(description = "성인 확인 및 서비스 이용약관 동의 여부", example = "true")
    @NotNull(message = "약관 동의는 필수입니다.")
    @AssertTrue(message = "만 19세 이상 성인 요건 및 이용약관에 동의해야 회원가입이 가능합니다.")
    private Boolean agreeToTerms;
}
