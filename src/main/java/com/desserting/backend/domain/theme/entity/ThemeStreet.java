package com.desserting.backend.domain.theme.entity;

import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "theme_streets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ThemeStreet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name; // 예: 앞산 카페거리, 김광석길

    @Column(length = 500)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String boundaryPolygonJson; // GeoJSON 점선 구역 폴리곤 좌표 배열

    @Column(precision = 10, scale = 7, nullable = false)
    private BigDecimal centerLatitude;

    @Column(precision = 10, scale = 7, nullable = false)
    private BigDecimal centerLongitude;

    @Column(length = 500)
    private String coverImageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public void updateTheme(String name, String description, String boundaryPolygonJson,
                            BigDecimal centerLatitude, BigDecimal centerLongitude,
                            String coverImageUrl, Boolean isActive) {
        if (name != null && !name.isBlank()) this.name = name;
        if (description != null) this.description = description;
        if (boundaryPolygonJson != null) this.boundaryPolygonJson = boundaryPolygonJson;
        if (centerLatitude != null) this.centerLatitude = centerLatitude;
        if (centerLongitude != null) this.centerLongitude = centerLongitude;
        if (coverImageUrl != null) this.coverImageUrl = coverImageUrl;
        if (isActive != null) this.isActive = isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
}
