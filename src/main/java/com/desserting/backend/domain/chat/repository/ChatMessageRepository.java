package com.desserting.backend.domain.chat.repository;

import com.desserting.backend.domain.chat.document.ChatMessageDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessageDocument, String> {

    List<ChatMessageDocument> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    List<ChatMessageDocument> findByRoomIdAndIdLessThanOrderByCreatedAtDesc(Long roomId, String lastMessageId, Pageable pageable);

    long countByRoomId(Long roomId);
}
