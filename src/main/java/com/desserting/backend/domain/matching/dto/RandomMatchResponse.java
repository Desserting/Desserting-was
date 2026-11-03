package com.desserting.backend.domain.matching.dto;

import com.desserting.backend.domain.matching.entity.MatchType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "랜덤 매칭 결과")
public class RandomMatchResponse {

    @Schema(description = "매칭 타입", example = "SAME_TASTE")
    private MatchType matchType;

    @Schema(description = "후보 존재 여부. false면 LOC-04 안내 문구(guide)를 표시", example = "true")
    private boolean found;

    @Schema(description = "매칭 상대 카드 (found=false면 null)")
    private MatchCandidateResponse candidate;

    @Schema(description = "실제 적용된 필터 방식", example = "DISTANCE")
    private AppliedFilter appliedFilter;

    @Schema(description = "오늘 이미 본 후보를 모두 소진해 처음부터 다시 보여주는 중인지 여부", example = "false")
    private boolean recycled;

    @Schema(description = "사용자 안내 문구 (후보 없음, 위치 권한 대체 등)")
    private String guide;

    public enum AppliedFilter {
        /** 내 좌표 기준 반경 검색 (LOC-01/02) */
        DISTANCE,
        /** 지역명 기준 검색 (LOC-03 수동 선택 또는 위치 미허용 대체) */
        REGION,
        /** 대구·경북 전체 */
        ALL
    }
}
