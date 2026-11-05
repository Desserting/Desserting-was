package com.desserting.backend.domain.premium.repository;

import com.desserting.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/** 프리미엄 여부 변경. User 엔티티·UserRepository(4-1)는 수정하지 않는다. */
public interface PremiumUserRepository extends Repository<User, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.isPremium = true WHERE u.id = :userId")
    int activatePremium(@Param("userId") Long userId);

    /**
     * 결제한 이용 기간이 모두 끝난 회원만 일반 회원으로 되돌린다.
     * 결제 내역이 없는 프리미엄(시드·운영자 지정)은 건드리지 않는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE User u SET u.isPremium = false
            WHERE u.isPremium = true
              AND u.id IN (SELECT s.user.id FROM PremiumSubscription s GROUP BY s.user.id HAVING MAX(s.expiresAt) <= :now)
            """)
    int expirePremium(@Param("now") LocalDateTime now);
}
