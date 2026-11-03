package com.desserting.backend.domain.chat.entity;

import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String roomUuid;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime lastMessageAt;

    @Column(length = 500)
    private String lastMessagePreview;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatRoomMember> members = new ArrayList<>();

    public static ChatRoom create() {
        return ChatRoom.builder()
                .roomUuid(UUID.randomUUID().toString())
                .isActive(true)
                .lastMessageAt(LocalDateTime.now())
                .build();
    }

    public void updateLastMessage(String preview) {
        this.lastMessagePreview = preview;
        this.lastMessageAt = LocalDateTime.now();
    }
}
