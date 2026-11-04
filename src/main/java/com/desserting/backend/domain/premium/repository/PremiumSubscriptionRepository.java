package com.desserting.backend.domain.premium.repository;

import com.desserting.backend.domain.premium.entity.PremiumSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PremiumSubscriptionRepository extends JpaRepository<PremiumSubscription, Long> {

    Optional<PremiumSubscription> findFirstByUserIdOrderByExpiresAtDesc(Long userId);

    List<PremiumSubscription> findAllByUserIdOrderByIdDesc(Long userId);
}
