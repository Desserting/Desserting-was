package com.desserting.backend.domain.user.repository;

import com.desserting.backend.domain.user.entity.ProfileVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfileVisitRepository extends JpaRepository<ProfileVisit, Long> {

    @Query("SELECT pv FROM ProfileVisit pv WHERE pv.visitor.id = :visitorId AND pv.visitedUser.id = :visitedUserId")
    Optional<ProfileVisit> findByVisitorIdAndVisitedUserId(@Param("visitorId") Long visitorId,
                                                          @Param("visitedUserId") Long visitedUserId);

    long countByVisitedUserId(Long visitedUserId);

    @Query("""
            SELECT pv FROM ProfileVisit pv
            JOIN FETCH pv.visitor v
            WHERE pv.visitedUser.id = :visitedUserId
            ORDER BY pv.visitedAt DESC
            """)
    List<ProfileVisit> findRecentVisitors(@Param("visitedUserId") Long visitedUserId);
}
