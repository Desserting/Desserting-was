package com.desserting.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 서비스 기준 시각(KST). 서버가 UTC로 떠도 예약 날짜·시간 판단이 어긋나지 않도록 주입해서 사용한다.
 */
@Configuration
public class ClockConfig {

    public static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(SERVICE_ZONE);
    }
}
