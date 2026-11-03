package com.desserting.backend.domain.matching.repository;

import com.desserting.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 랜덤 매칭 후보 조회 전용 리포지토리.
 * UserRepository(4-1 담당)와 파일을 분리해 병렬 작업 시 충돌을 피한다.
 * 일반 회원(ROLE_USER)만 후보가 되며, 취향·성별·거리 세부 조건은 서비스에서 거른다.
 */
public interface MatchCandidateRepository extends Repository<User, Long> {

    @Query("""
            SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.dessertTags
            WHERE u.id <> :me
              AND u.role = com.desserting.backend.domain.user.entity.UserRole.ROLE_USER
            """)
    List<User> findAllCandidates(@Param("me") Long me);

    @Query("""
            SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.dessertTags
            WHERE u.id <> :me
              AND u.role = com.desserting.backend.domain.user.entity.UserRole.ROLE_USER
              AND u.region LIKE CONCAT(:regionPrefix, '%')
            """)
    List<User> findCandidatesByRegion(@Param("me") Long me, @Param("regionPrefix") String regionPrefix);

    @Query("""
            SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.dessertTags
            WHERE u.id <> :me
              AND u.role = com.desserting.backend.domain.user.entity.UserRole.ROLE_USER
              AND u.latitude BETWEEN :minLat AND :maxLat
              AND u.longitude BETWEEN :minLng AND :maxLng
            """)
    List<User> findCandidatesInBox(@Param("me") Long me,
                                   @Param("minLat") BigDecimal minLat, @Param("maxLat") BigDecimal maxLat,
                                   @Param("minLng") BigDecimal minLng, @Param("maxLng") BigDecimal maxLng);
}
