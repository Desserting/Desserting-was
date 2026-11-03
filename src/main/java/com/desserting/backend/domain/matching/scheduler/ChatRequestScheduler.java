package com.desserting.backend.domain.matching.scheduler;

import com.desserting.backend.domain.matching.service.ChatRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRequestScheduler {

    private final ChatRequestService chatRequestService;

    // 매 5분마다 24시간이 경과한 미응답 PENDING 대화 요청 자동 EXPIRED 처리 및 하트 100% 환불 (HRT-03)
    @Scheduled(cron = "0 */5 * * * *")
    public void processExpiredChatRequests() {
        int expiredCount = chatRequestService.expireStaleRequests();
        if (expiredCount > 0) {
            log.info("만료 대화 요청 정리 스케줄러: {}건 만료 및 환불 완료", expiredCount);
        }
    }
}
