package com.desserting.backend.domain.heart.repository;

import com.desserting.backend.domain.heart.entity.HeartHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HeartHistoryRepository extends JpaRepository<HeartHistory, Long> {
    List<HeartHistory> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
