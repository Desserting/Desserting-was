package com.desserting.backend.domain.user.service;

import com.desserting.backend.domain.user.dto.OtherUserProfileResponse;
import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.domain.user.entity.UserBlock;
import com.desserting.backend.domain.user.entity.UserLike;
import com.desserting.backend.domain.user.entity.UserReport;
import com.desserting.backend.domain.user.repository.UserBlockRepository;
import com.desserting.backend.domain.user.repository.UserLikeRepository;
import com.desserting.backend.domain.user.repository.UserReportRepository;
import com.desserting.backend.domain.user.repository.UserRepository;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialService {

    private final UserRepository userRepository;
    private final UserLikeRepository userLikeRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserReportRepository userReportRepository;

    @Transactional
    public boolean toggleLike(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "본인에게는 좋아요를 누를 수 없습니다.");
        }

        if (userBlockRepository.isBlockedBetween(senderId, receiverId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "차단 관계인 사용자에게는 좋아요를 누를 수 없습니다.");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Optional<UserLike> existingLike = userLikeRepository.findBySenderIdAndReceiverId(senderId, receiverId);
        if (existingLike.isPresent()) {
            userLikeRepository.delete(existingLike.get());
            log.info("좋아요 취소: senderId={}, receiverId={}", senderId, receiverId);
            return false;
        } else {
            userLikeRepository.save(UserLike.of(sender, receiver));
            log.info("좋아요 등록: senderId={}, receiverId={}", senderId, receiverId);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public List<OtherUserProfileResponse> getMyLikesReceived(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // LIKE-02: 나를 좋아요한 사람 확인은 프리미엄 전용 혜택
        if (!Boolean.TRUE.equals(user.getIsPremium())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "나를 좋아요한 사람 확인은 프리미엄 패스 회원 전용 기능입니다.");
        }

        List<UserLike> likes = userLikeRepository.findLikesReceivedWithSender(userId);
        return likes.stream()
                .filter(like -> !userBlockRepository.isBlockedBetween(userId, like.getSender().getId()))
                .map(like -> OtherUserProfileResponse.from(like.getSender()))
                .toList();
    }

    @Transactional
    public void blockUser(Long blockerId, Long blockedId, String reason) {
        if (blockerId.equals(blockedId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "본인을 차단할 수 없습니다.");
        }

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            userBlockRepository.save(UserBlock.of(blocker, blocked, reason));
        }
        log.info("사용자 차단 완료: blockerId={}, blockedId={}", blockerId, blockedId);
    }

    @Transactional
    public void reportUser(Long reporterId, Long reportedId, String reason, String description) {
        if (reporterId.equals(reportedId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "본인을 신고할 수 없습니다.");
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User reported = userRepository.findById(reportedId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        userReportRepository.save(UserReport.of(reporter, reported, reason, description));
        log.info("사용자 신고 접수: reporterId={}, reportedId={}, reason={}", reporterId, reportedId, reason);
    }
}
