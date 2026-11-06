package com.desserting.backend.domain.user.repository;

import com.desserting.backend.domain.user.entity.UserLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserLikeRepository extends JpaRepository<UserLike, Long> {

    boolean existsBySenderIdAndReceiverId(Long senderId, Long receiverId);

    Optional<UserLike> findBySenderIdAndReceiverId(Long senderId, Long receiverId);

    @Query("SELECT ul.receiver.id FROM UserLike ul WHERE ul.sender.id = :senderId")
    Set<Long> findLikedReceiverIdsBySenderId(@Param("senderId") Long senderId);

    @Query("SELECT DISTINCT ul FROM UserLike ul " +
            "JOIN FETCH ul.sender s " +
            "LEFT JOIN FETCH s.dessertTags " +
            "WHERE ul.receiver.id = :receiverId " +
            "AND s.role = com.desserting.backend.domain.user.entity.UserRole.ROLE_USER " +
            "ORDER BY ul.createdAt DESC")
    List<UserLike> findLikesReceivedWithSender(@Param("receiverId") Long receiverId);

    @Query("SELECT DISTINCT ul FROM UserLike ul " +
            "JOIN FETCH ul.receiver rc " +
            "LEFT JOIN FETCH rc.dessertTags " +
            "WHERE ul.sender.id = :senderId " +
            "AND rc.role = com.desserting.backend.domain.user.entity.UserRole.ROLE_USER " +
            "ORDER BY ul.createdAt DESC")
    List<UserLike> findLikesSentWithReceiver(@Param("senderId") Long senderId);
}

