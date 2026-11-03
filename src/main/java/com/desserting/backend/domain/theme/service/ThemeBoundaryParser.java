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

    /** [{lat, lng}] → GeoJSON Polygon 문자열 (시드 데이터·관리자 등록용). 닫힌 고리가 아니면 첫 점을 끝에 덧붙인다. */
    public String toGeoJson(List<Coordinate> boundary) {
        List<double[]> ring = new ArrayList<>();
        for (Coordinate c : boundary) {
            ring.add(new double[]{c.lng(), c.lat()});
        }
        if (!boundary.isEmpty() && !boundary.get(0).equals(boundary.get(boundary.size() - 1))) {
            ring.add(new double[]{boundary.get(0).lng(), boundary.get(0).lat()});
        }
        try {
            return objectMapper.writeValueAsString(
                    java.util.Map.of("type", "Polygon", "coordinates", List.of(ring)));
        } catch (Exception e) {
            throw new IllegalStateException("GeoJSON 직렬화 실패", e);
        }
    }
}
