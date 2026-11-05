package com.desserting.backend.domain.premium.scheduler;

import com.desserting.backend.domain.premium.service.PremiumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PremiumExpiryScheduler {

    private final PremiumService premiumService;

    /** 10분마다 만료된 프리미엄 패스를 정리한다 */
    @Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
    public void expire() {
        int expired = premiumService.expireEndedSubscriptions();
        if (expired > 0) {
            log.info("프리미엄 패스 만료 처리: {}명", expired);
        }
    }
}
