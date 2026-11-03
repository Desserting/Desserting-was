package com.desserting.backend.domain.user.repository;

import com.desserting.backend.domain.user.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("SELECT b.blocked.id FROM UserBlock b WHERE b.blocker.id = :userId")
    List<Long> findBlockedUserIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT b.blocker.id FROM UserBlock b WHERE b.blocked.id = :userId")
    List<Long> findBlockerUserIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM UserBlock b " +
            "WHERE (b.blocker.id = :userAId AND b.blocked.id = :userBId) " +
            "   OR (b.blocker.id = :userBId AND b.blocked.id = :userAId)")
    boolean isBlockedBetween(@Param("userAId") Long userAId, @Param("userBId") Long userBId);
}
