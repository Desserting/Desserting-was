package com.desserting.backend.domain.chat.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ChatMessageDocument {

    @Id
    private String id;

    @Indexed
    private Long roomId;

    @Indexed
    private Long senderId;

    private String senderNickname;

    private String messageType; // TEXT, IMAGE, PLACE_PROPOSAL, SYSTEM

    private String content;

    private Map<String, Object> metadata; // 매장 제안 정보 (storeId, storeName, date 등)

    @Builder.Default
    private List<Long> readUserIds = new ArrayList<>();

    @Indexed
    private LocalDateTime createdAt;
}
