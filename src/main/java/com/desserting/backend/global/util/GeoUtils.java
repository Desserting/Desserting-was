package com.desserting.backend.global.util;

import java.math.BigDecimal;

/**
 * 위경도 거리 계산 유틸 (PostGIS 없이 하버사인 공식 사용).
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double KM_PER_LAT_DEGREE = 111.32;

    private GeoUtils() {
    }

    /** 두 좌표 사이 거리(km). */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static double distanceKm(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        return distanceKm(lat1.doubleValue(), lng1.doubleValue(), lat2.doubleValue(), lng2.doubleValue());
    }

    /** 반경 검색 전 DB 1차 필터용 위도 범위(±도). */
    public static double latDelta(double radiusKm) {
        return radiusKm / KM_PER_LAT_DEGREE;
    }

    /** 반경 검색 전 DB 1차 필터용 경도 범위(±도). */
    public static double lngDelta(double radiusKm, double atLat) {
        return radiusKm / (KM_PER_LAT_DEGREE * Math.cos(Math.toRadians(atLat)));
    }

    /** 소수 첫째 자리 반올림. */
    public static double round1(double km) {
        return Math.round(km * 10) / 10.0;
    }
}
