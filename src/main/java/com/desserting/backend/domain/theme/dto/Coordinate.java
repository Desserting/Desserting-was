package com.desserting.backend.domain.theme.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "위경도 좌표")
public record Coordinate(
        @Schema(description = "위도", example = "35.8295") double lat,
        @Schema(description = "경도", example = "128.5875") double lng
) {
}
