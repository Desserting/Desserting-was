package com.desserting.backend.domain.matching.dto;

import com.desserting.backend.domain.user.entity.DessertType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "랜덤 매칭 상대 카드 (정확한 위치 제외)")
public class MatchCandidateResponse {

    @Schema(description = "상대방 사용자 ID (대화 요청 시 사용)", example = "2")
    private Long userId;

    @Schema(description = "디저트 닉네임", example = "딸기케이크러버")
    private String nickname;

    @Schema(description = "나이", example = "24")
    private Integer age;

    @Schema(description = "성별", example = "FEMALE")
    private String gender;

    @Schema(description = "활동 지역", example = "대구 남구")
    private String region;

    @Schema(description = "한 줄 자기소개")
    private String bio;

    @Schema(description = "디저트 대표 아바타 이미지 URL")
    private String dessertAvatarUrl;

    @Schema(description = "상대방 선호 디저트 목록")
    private List<DessertType> dessertTags;

    @Schema(description = "나와 겹치는 디저트 취향", example = "[\"CAKE\"]")
    private List<DessertType> commonDessertTags;

    @Schema(description = "대략적인 거리(km, 올림 정수). 위치정보가 없으면 null", example = "5")
    private Integer approxDistanceKm;

    @Schema(description = "내가 상대방에게 보낸 대기 중인 대화 요청이 있는지 여부", example = "false")
    private Boolean requested;

    @Schema(description = "내가 상대방을 좋아요(Like)했는지 여부", example = "false")
    private Boolean liked;

    @Schema(description = "내가 상대방에게 대화 요청을 보낼 수 있는 상태인지 여부 (차단, 중복, 하트/권한 등 종합 판정)", example = "true")
    private Boolean canRequest;
}
