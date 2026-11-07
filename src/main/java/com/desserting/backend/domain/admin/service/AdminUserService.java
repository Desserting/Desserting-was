package com.desserting.backend.domain.admin.service;

import com.desserting.backend.domain.admin.dto.AdminUserResponse;
import com.desserting.backend.domain.admin.dto.HeartGrantRequest;
import com.desserting.backend.domain.heart.entity.HeartHistory;
import com.desserting.backend.domain.heart.entity.HeartHistoryType;
import com.desserting.backend.domain.heart.repository.HeartHistoryRepository;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final HeartHistoryRepository heartHistoryRepository;

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAllWithTags().stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse grantHearts(Long userId, HeartGrantRequest request) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        int amount = request.getAmount();
        user.addHeart(amount);

        String description = request.getReason() != null && !request.getReason().isBlank()
                ? "관리자 하트 즉시 지급 (" + request.getReason() + ")"
                : "관리자 하트 즉시 지급 (+" + amount + "개)";

        heartHistoryRepository.save(HeartHistory.of(
                user,
                amount,
                user.getHeartCount(),
                HeartHistoryType.ADMIN_GRANT,
                description
        ));

        log.info("관리자 하트 지급 완료: userId={}, +{}, 현재 하트={}", userId, amount, user.getHeartCount());
        return AdminUserResponse.from(user);
    }
}
