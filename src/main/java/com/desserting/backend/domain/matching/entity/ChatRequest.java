package com.desserting.backend.domain.matching.entity;

import com.desserting.backend.domain.user.entity.User;
import com.desserting.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    public static ChatRequest create(User sender, User receiver) {
        return ChatRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(RequestStatus.PENDING)
                .expiredAt(LocalDateTime.now().plusHours(24)) // 24시간 동안 유효
                .build();
    }

    public void accept() {
        if (this.status != RequestStatus.PENDING) {
            throw new IllegalStateException("대기 중인 요청만 수락할 수 있습니다.");
        }
        this.status = RequestStatus.ACCEPTED;
    }

    public void reject() {
        if (this.status != RequestStatus.PENDING) {
            throw new IllegalStateException("대기 중인 요청만 거절할 수 있습니다.");
        }
        this.status = RequestStatus.REJECTED;
    }

    public void expire() {
        this.status = RequestStatus.EXPIRED;
    }
}
