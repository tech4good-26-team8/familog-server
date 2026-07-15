package com.familog.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.familog.server.domain.MessageRead;

public interface MessageReadRepository extends JpaRepository<MessageRead, Long> {

    /**
     * 읽음 처리 한 방 쿼리. INSERT IGNORE가 (message_id, reader_id) 유니크 제약으로
     * 이미 읽은 건을 원자적으로 걸러줘서 동시 호출에도 멱등 (check-then-insert 레이스 없음).
     * senderId가 null이면 그룹 전체, 있으면 그 멤버 메시지만.
     */
    @Modifying
    @Query(value = """
            insert ignore into message_read (message_id, reader_id, read_at)
            select m.id, :readerId, now()
            from message m
            where m.group_id = :groupId
              and m.sender_id <> :readerId
              and (:senderId is null or m.sender_id = :senderId)
            """, nativeQuery = true)
    int markAllRead(@Param("groupId") Long groupId, @Param("readerId") Long readerId, @Param("senderId") Long senderId);
}
