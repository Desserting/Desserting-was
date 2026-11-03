package com.desserting.backend.domain.heart.scheduler;

import com.desserting.backend.domain.heart.service.HeartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyHeartScheduler {

    private final HeartService heartService;

    // 매일 자정(00:00:00) 한국 표준시 기준 일반 회원에게 5개 무료 하트 충전 (HRT-01)
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void resetDailyHearts() {
        log.info("자정 무료 하트 일괄 충전 스케줄러 실행");
        int chargedCount = heartService.chargeDailyHearts();
        log.info("자정 무료 하트 충전 성공 대상자: {}명", chargedCount);
    }
}
