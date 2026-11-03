package com.desserting.backend.domain.matching.repository;

import com.desserting.backend.domain.matching.entity.ChatRequest;
import com.desserting.backend.domain.matching.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRequestRepository extends JpaRepository<ChatRequest, Long> {

    @Query("SELECT r FROM ChatRequest r " +
            "JOIN FETCH r.sender s " +
            "LEFT JOIN FETCH s.dessertTags " +
            "WHERE r.receiver.id = :receiverId AND r.status = :status " +
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

    boolean existsBySenderIdAndReceiverIdAndStatus(Long senderId, Long receiverId, RequestStatus status);
}
