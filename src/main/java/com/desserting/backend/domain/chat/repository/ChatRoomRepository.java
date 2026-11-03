package com.desserting.backend.domain.chat.repository;

import com.desserting.backend.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByRoomUuid(String roomUuid);

    @Query("SELECT r FROM ChatRoom r " +
            "JOIN FETCH r.members m " +
            "JOIN FETCH m.user u " +
            "WHERE r.id = :roomId")
    Optional<ChatRoom> findByIdWithMembers(@Param("roomId") Long roomId);
}
