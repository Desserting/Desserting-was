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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocialServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserLikeRepository userLikeRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private UserReportRepository userReportRepository;

    @InjectMocks
    private SocialService socialService;

    @Test
    @DisplayName("좋아요가 없던 상대에게 토글 시 새로 등록되고 true가 반환된다")
    void toggleLike_Register() {
        // given
        User sender = User.builder().email("sender@desserting.com").nickname("보낸이").build();
        ReflectionTestUtils.setField(sender, "id", 1L);

        User receiver = User.builder().email("receiver@desserting.com").nickname("받는이").build();
        ReflectionTestUtils.setField(receiver, "id", 2L);

        given(userRepository.findById(1L)).willReturn(Optional.of(sender));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(userLikeRepository.findBySenderIdAndReceiverId(1L, 2L)).willReturn(Optional.empty());

        // when
        boolean isLiked = socialService.toggleLike(1L, 2L);

        // then
        assertThat(isLiked).isTrue();
        verify(userLikeRepository).save(any(UserLike.class));
    }

    @Test
    @DisplayName("이미 좋아요한 상대에게 토글 시 삭제되고 false가 반환된다")
    void toggleLike_Cancel() {
        // given
        User sender = User.builder().email("sender@desserting.com").nickname("보낸이").build();
        ReflectionTestUtils.setField(sender, "id", 1L);

        User receiver = User.builder().email("receiver@desserting.com").nickname("받는이").build();
        ReflectionTestUtils.setField(receiver, "id", 2L);

        UserLike existingLike = UserLike.of(sender, receiver);

        given(userRepository.findById(1L)).willReturn(Optional.of(sender));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(userLikeRepository.findBySenderIdAndReceiverId(1L, 2L)).willReturn(Optional.of(existingLike));

        // when
        boolean isLiked = socialService.toggleLike(1L, 2L);

        // then
        assertThat(isLiked).isFalse();
        verify(userLikeRepository).delete(existingLike);
    }

    @Test
    @DisplayName("차단 관계인 상대방에게 좋아요 시 ACCESS_DENIED 예외 발생")
    void toggleLike_ThrowsException_WhenBlocked() {
        // given
        given(userBlockRepository.isBlockedBetween(1L, 2L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> socialService.toggleLike(1L, 2L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("본인에게 좋아요 시 INVALID_INPUT_VALUE 예외 발생")
    void toggleLike_SelfLike() {
        // when & then
        assertThatThrownBy(() -> socialService.toggleLike(1L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("일반 회원이 나를 좋아요한 사람 목록 조회 시 ACCESS_DENIED 예외 발생")
    void getMyLikesReceived_NotPremium() {
        // given
        User regularUser = User.builder().email("user@desserting.com").nickname("일반유저").build();
        ReflectionTestUtils.setField(regularUser, "id", 1L);
        ReflectionTestUtils.setField(regularUser, "isPremium", false);

        given(userRepository.findById(1L)).willReturn(Optional.of(regularUser));

        // when & then
        assertThatThrownBy(() -> socialService.getMyLikesReceived(1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("프리미엄 회원이 나를 좋아요한 사람 목록 조회 시 성공적으로 프로필 목록 반환")
    void getMyLikesReceived_PremiumSuccess() {
        // given
        User premiumUser = User.builder().email("vip@desserting.com").nickname("VIP").build();
        ReflectionTestUtils.setField(premiumUser, "id", 1L);
        ReflectionTestUtils.setField(premiumUser, "isPremium", true);

        User sender = User.builder().email("sender@desserting.com").nickname("팬1").dessertTags(new ArrayList<>()).build();
        ReflectionTestUtils.setField(sender, "id", 2L);

        UserLike like = UserLike.of(sender, premiumUser);

        given(userRepository.findById(1L)).willReturn(Optional.of(premiumUser));
        given(userLikeRepository.findLikesReceivedWithSender(1L)).willReturn(List.of(like));

        // when
        List<OtherUserProfileResponse> result = socialService.getMyLikesReceived(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(2L);
        assertThat(result.get(0).getNickname()).isEqualTo("팬1");
    }

    @Test
    @DisplayName("사용자 차단 성공")
    void blockUser_Success() {
        // given
        User blocker = User.builder().nickname("차단자").build();
        ReflectionTestUtils.setField(blocker, "id", 1L);

        User blocked = User.builder().nickname("차단대상").build();
        ReflectionTestUtils.setField(blocked, "id", 2L);

        given(userRepository.findById(1L)).willReturn(Optional.of(blocker));
        given(userRepository.findById(2L)).willReturn(Optional.of(blocked));
        given(userBlockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).willReturn(false);

        // when
        socialService.blockUser(1L, 2L, "비매너 대화");

        // then
        verify(userBlockRepository).save(any(UserBlock.class));
    }

    @Test
    @DisplayName("사용자 신고 성공")
    void reportUser_Success() {
        // given
        User reporter = User.builder().nickname("신고자").build();
        ReflectionTestUtils.setField(reporter, "id", 1L);

        User reported = User.builder().nickname("피신고자").build();
        ReflectionTestUtils.setField(reported, "id", 2L);

        given(userRepository.findById(1L)).willReturn(Optional.of(reporter));
        given(userRepository.findById(2L)).willReturn(Optional.of(reported));

        // when
        socialService.reportUser(1L, 2L, "SPAM", "부적절한 광고 링크 전송");

        // then
        verify(userReportRepository).save(any(UserReport.class));
    }
}
