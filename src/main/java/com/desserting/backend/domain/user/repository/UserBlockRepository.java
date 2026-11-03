package com.desserting.backend.domain.user.repository;

import com.desserting.backend.domain.user.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}
