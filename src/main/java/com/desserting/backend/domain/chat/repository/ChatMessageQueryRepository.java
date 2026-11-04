package com.desserting.backend.domain.chat.repository;

import com.desserting.backend.domain.chat.document.ChatMessageDocument;
import com.desserting.backend.global.error.CustomException;
import com.desserting.backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 채팅 메시지 페이지 조회·읽음 처리.
 * 정렬과 커서를 모두 _id(ObjectId) 기준으로 맞춰, createdAt 이 같거나 뒤바뀐 메시지도 페이지 사이에서 빠지지 않게 한다.
 */
@Repository
@RequiredArgsConstructor
public class ChatMessageQueryRepository {

    private final MongoTemplate mongoTemplate;

    /** 최신순 한 페이지. cursorId 가 있으면 그보다 오래된 메시지만 */
    public List<ChatMessageDocument> findPage(Long roomId, String cursorId, int size) {
        Criteria criteria = Criteria.where("roomId").is(roomId);
        if (cursorId != null && !cursorId.isBlank()) {
            if (!ObjectId.isValid(cursorId)) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 cursorId 입니다.");
            }
            criteria = criteria.and("_id").lt(new ObjectId(cursorId));
        }
        Query query = Query.query(criteria).with(Sort.by(Sort.Direction.DESC, "_id")).limit(size);
        return mongoTemplate.find(query, ChatMessageDocument.class);
    }

    /** 상대가 보낸 메시지 중 내가 아직 읽지 않은 개수 */
    public long countUnread(Long roomId, Long userId) {
        return mongoTemplate.count(unreadQuery(roomId, userId), ChatMessageDocument.class);
    }

    /** 방의 안 읽은 메시지를 모두 읽음으로 표시 */
    public void markAllRead(Long roomId, Long userId) {
        mongoTemplate.updateMulti(unreadQuery(roomId, userId), new Update().addToSet("readUserIds", userId),
                ChatMessageDocument.class);
    }

    private Query unreadQuery(Long roomId, Long userId) {
        return Query.query(Criteria.where("roomId").is(roomId)
                .and("senderId").ne(userId)
                .and("readUserIds").ne(userId));
    }
}
