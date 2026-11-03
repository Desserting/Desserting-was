package com.desserting.backend.domain.heart.repository;

import com.desserting.backend.domain.heart.entity.HeartPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HeartPurchaseRepository extends JpaRepository<HeartPurchase, Long> {
    List<HeartPurchase> findAllByUserIdOrderByIdDesc(Long userId);
}
