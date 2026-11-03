package com.desserting.backend.domain.admin.service;

import com.desserting.backend.domain.admin.dto.ThemeStreetCreateRequest;
import com.desserting.backend.domain.admin.dto.ThemeStreetResponse;
import com.desserting.backend.domain.admin.dto.ThemeStreetUpdateRequest;
import com.desserting.backend.domain.theme.entity.ThemeStreet;
import com.desserting.backend.domain.theme.repository.ThemeStreetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminThemeServiceTest {

    @Mock
    private ThemeStreetRepository themeStreetRepository;

    @InjectMocks
    private AdminThemeService adminThemeService;

    @Test
    @DisplayName("테마거리 신규 등록 시 GeoJSON 폴리곤과 함께 정상 저장")
    void createThemeStreet_Success() {
        // given
        String polygon = "{\"type\":\"Polygon\",\"coordinates\":[[[128.58,35.83],[128.59,35.83],[128.59,35.84],[128.58,35.84],[128.58,35.83]]]}";
        ThemeStreetCreateRequest request = ThemeStreetCreateRequest.builder()
                .name("앞산 카페거리")
                .description("앞산 조망 디저트 카페 거리")
                .boundaryPolygonJson(polygon)
                .centerLatitude(new BigDecimal("35.8324000"))
                .centerLongitude(new BigDecimal("128.5831000"))
                .isActive(true)
                .build();

        given(themeStreetRepository.save(any(ThemeStreet.class))).willAnswer(invocation -> {
            ThemeStreet t = invocation.getArgument(0);
            ReflectionTestUtils.setField(t, "id", 1L);
            return t;
        });

        // when
        ThemeStreetResponse response = adminThemeService.createThemeStreet(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("앞산 카페거리");
        assertThat(response.getBoundaryPolygonJson()).contains("Polygon");
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("테마거리 정보 수정 성공")
    void updateThemeStreet_Success() {
        // given
        ThemeStreet themeStreet = ThemeStreet.builder()
                .name("구 테마거리")
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(themeStreet, "id", 1L);

        given(themeStreetRepository.findById(1L)).willReturn(Optional.of(themeStreet));

        ThemeStreetUpdateRequest request = ThemeStreetUpdateRequest.builder()
                .name("김광석 다시그리기길 카페거리")
                .isActive(false)
                .build();

        // when
        ThemeStreetResponse response = adminThemeService.updateThemeStreet(1L, request);

        // then
        assertThat(response.getName()).isEqualTo("김광석 다시그리기길 카페거리");
        assertThat(response.getIsActive()).isFalse();
    }
}
