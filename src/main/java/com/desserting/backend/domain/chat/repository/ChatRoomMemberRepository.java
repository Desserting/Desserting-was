package com.desserting.backend.domain.chat.repository;

import com.desserting.backend.domain.chat.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    boolean existsByChatRoomIdAndUserIdAndIsLeftFalse(Long chatRoomId, Long userId);

    @Query("SELECT m FROM ChatRoomMember m " +
            "JOIN FETCH m.chatRoom r " +
            "WHERE m.user.id = :userId AND m.isLeft = false " +
            "ORDER BY r.lastMessageAt DESC NULLS LAST")
    List<ChatRoomMember> findAllMyActiveMemberships(@Param("userId") Long userId);

    @Query("SELECT m FROM ChatRoomMember m " +
            "JOIN FETCH m.user u " +
            "LEFT JOIN FETCH u.dessertTags " +
            "WHERE m.chatRoom.id = :chatRoomId AND m.user.id <> :excludeUserId")
    Optional<ChatRoomMember> findOtherMember(
            @Param("chatRoomId") Long chatRoomId,
            @Param("excludeUserId") Long excludeUserId
    );

    @Query("SELECT CASE WHEN COUNT(m1) > 0 THEN true ELSE false END " +
            "FROM ChatRoomMember m1, ChatRoomMember m2 " +
            "WHERE m1.chatRoom.id = m2.chatRoom.id " +
            "AND m1.user.id = :userAId AND m2.user.id = :userBId " +
            "AND m1.isLeft = false AND m2.isLeft = false")
    boolean existsActiveRoomBetween(
            @Param("userAId") Long userAId,
            @Param("userBId") Long userBId
    );
}
