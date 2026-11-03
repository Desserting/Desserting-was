package com.desserting.backend.domain.user.entity;

import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 4-1. 내 프로필 방문자 기록 (PRF-05)
 * 타인이 내 프로필을 열람했을 때 방문 로그를 적재하고, 프리미엄 회원에게 방문자 정보를 제공한다.
 */
@Entity
@Table(
        name = "profile_visits",
        uniqueConstraints = @UniqueConstraint(name = "uk_profile_visit", columnNames = {"visited_user_id", "visitor_id"}),
        indexes = @Index(name = "idx_profile_visit_user", columnList = "visited_user_id, updated_at DESC")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProfileVisit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visited_user_id", nullable = false)
    private User visitedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visitor_id", nullable = false)
    private User visitor;

    @Column(nullable = false)
    private LocalDateTime visitedAt;

    public static ProfileVisit of(User visitedUser, User visitor) {
        LocalDateTime now = LocalDateTime.now();
        return ProfileVisit.builder()
                .visitedUser(visitedUser)
                .visitor(visitor)
                .visitedAt(now)
                .build();
    }

    public void updateVisitedAt() {
        this.visitedAt = LocalDateTime.now();
    }
}
