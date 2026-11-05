package com.desserting.backend.domain.matching.repository;

import com.desserting.backend.domain.matching.entity.ChatRequest;
import com.desserting.backend.domain.matching.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatRequestRepository extends JpaRepository<ChatRequest, Long> {

    @Query("SELECT DISTINCT r FROM ChatRequest r " +
            "JOIN FETCH r.sender s " +
            "LEFT JOIN FETCH s.dessertTags " +
            "WHERE r.receiver.id = :receiverId " +
            "AND r.status = :status " +
            "AND r.expiredAt > CURRENT_TIMESTAMP " +
            "ORDER BY r.createdAt DESC")
    List<ChatRequest> findReceivedRequestsWithSender(
            @Param("receiverId") Long receiverId,
            @Param("status") RequestStatus status
    );

    @Query("SELECT r FROM ChatRequest r " +
            "JOIN FETCH r.sender s " +
            "JOIN FETCH r.receiver rc " +
            "WHERE r.id = :id")
    Optional<ChatRequest> findByIdWithUsers(@Param("id") Long id);

    @Query("SELECT DISTINCT r FROM ChatRequest r " +
            "JOIN FETCH r.sender s " +
            "LEFT JOIN FETCH s.dessertTags " +
            "WHERE r.receiver.id = :receiverId AND r.createdAt >= :since " +
            "ORDER BY r.createdAt DESC")
    List<ChatRequest> findRecentReceivedWithSender(@Param("receiverId") Long receiverId,
                                                   @Param("since") LocalDateTime since);

    @Query("SELECT DISTINCT r FROM ChatRequest r " +
            "JOIN FETCH r.receiver rc " +
            "LEFT JOIN FETCH rc.dessertTags " +
            "WHERE r.sender.id = :senderId " +
            "AND r.status = :status " +
            "AND r.expiredAt > CURRENT_TIMESTAMP " +
            "ORDER BY r.createdAt DESC")
    List<ChatRequest> findSentRequestsWithReceiver(
            @Param("senderId") Long senderId,
            @Param("status") RequestStatus status
    );

    @Query("SELECT DISTINCT r FROM ChatRequest r " +
            "JOIN FETCH r.receiver rc " +
            "LEFT JOIN FETCH rc.dessertTags " +
            "WHERE r.sender.id = :senderId AND r.createdAt >= :since " +
            "ORDER BY r.createdAt DESC")
    List<ChatRequest> findRecentSentWithReceiver(@Param("senderId") Long senderId,
                                                @Param("since") LocalDateTime since);

    boolean existsBySenderIdAndReceiverIdAndStatus(Long senderId, Long receiverId, RequestStatus status);

    @Query("SELECT r FROM ChatRequest r WHERE r.status = :status AND r.expiredAt <= :now")
    List<ChatRequest> findAllByStatusAndExpiredAtLessThanEqual(
            @Param("status") RequestStatus status,
            @Param("now") LocalDateTime now
    );
}
