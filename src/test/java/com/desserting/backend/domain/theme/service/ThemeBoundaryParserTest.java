package com.desserting.backend.domain.theme.service;

import com.desserting.backend.domain.theme.dto.Coordinate;
import com.desserting.backend.global.util.GeoUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ThemeBoundaryParserTest {

    private final ThemeBoundaryParser parser = new ThemeBoundaryParser(new ObjectMapper());

    @Test
    @DisplayName("GeoJSON [lng, lat] 순서를 {lat, lng}로 변환한다")
    void parse() {
        String geoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[128.5801,35.8302],[128.5915,35.8302],[128.5915,35.8385],[128.5801,35.8302]]]}";

        List<Coordinate> parsed = parser.parse(geoJson);

        assertThat(parsed).hasSize(4);
        assertThat(parsed.get(0)).isEqualTo(new Coordinate(35.8302, 128.5801));
        assertThat(parsed.get(2)).isEqualTo(new Coordinate(35.8385, 128.5915));
    }

    @Test
    @DisplayName("비어 있거나 깨진 JSON은 빈 목록")
    void invalidJson() {
        assertThat(parser.parse(null)).isEmpty();
        assertThat(parser.parse("not-json")).isEmpty();
    }

    @Test
    @DisplayName("하버사인 거리: 앞산 카페거리 ↔ 수성못 약 2.9km")
    void haversine() {
        double km = GeoUtils.distanceKm(35.8290, 128.5852, 35.8281, 128.6172);
        assertThat(km).isCloseTo(2.89, within(0.05));
    }
}
