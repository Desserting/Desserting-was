package com.desserting.backend.domain.user.repository;

import com.desserting.backend.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.dessertTags ORDER BY u.id ASC")
    List<User> findAllWithTags();

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.dessertTags WHERE u.role = com.desserting.backend.domain.user.entity.UserRole.ROLE_USER ORDER BY u.id ASC")
    List<User> findAllNormalUsersWithTags();

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.dessertTags WHERE u.id IN :ids")
    List<User> findAllByIdInWithTags(@Param("ids") Collection<Long> ids);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.dessertTags WHERE u.id = :id")
    Optional<User> findByIdWithTags(@Param("id") Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.dessertTags WHERE u.email = :email")
    Optional<User> findByEmailWithTags(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithLock(@Param("id") Long id);

    @Modifying
    @Query("UPDATE User u SET u.heartCount = 5 WHERE u.isPremium = false AND u.heartCount < 5")
    int resetDailyHeartsForUsers();
}
