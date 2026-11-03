package com.desserting.backend.domain.user.repository;

import com.desserting.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.dessertTags WHERE u.id = :id")
    Optional<User> findByIdWithTags(@Param("id") Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.dessertTags WHERE u.email = :email")
    Optional<User> findByEmailWithTags(@Param("email") String email);
}
