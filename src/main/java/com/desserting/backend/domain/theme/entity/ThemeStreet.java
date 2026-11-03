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
}
