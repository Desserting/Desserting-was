package com.desserting.backend.domain.theme.service;

import com.desserting.backend.domain.theme.dto.Coordinate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ThemeStreet.boundaryPolygonJson (GeoJSON Polygon) → 지도 SDK용 [{lat, lng}] 변환.
 * 저장 형식: {"type":"Polygon","coordinates":[[[lng,lat],[lng,lat],...]]}  (GeoJSON 표준: 경도, 위도 순서)
 * 바깥 고리(첫 번째 ring)만 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThemeBoundaryParser {

    private final ObjectMapper objectMapper;

    public List<Coordinate> parse(String geoJson) {
        if (geoJson == null || geoJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(geoJson);
            JsonNode outerRing = root.path("coordinates").path(0);
            List<Coordinate> result = new ArrayList<>();
            for (JsonNode point : outerRing) {
                result.add(new Coordinate(point.get(1).asDouble(), point.get(0).asDouble()));
            }
            return result;
        } catch (Exception e) {
            log.warn("테마거리 경계 GeoJSON 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }
}
