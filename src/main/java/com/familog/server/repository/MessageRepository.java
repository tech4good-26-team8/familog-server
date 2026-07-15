package com.familog.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.familog.server.domain.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByGroupIdAndIdGreaterThanOrderByIdAsc(Long groupId, Long afterId);

    List<Message> findByGroupIdOrderByIdAsc(Long groupId);

    List<Message> findBySenderIdOrderByIdAsc(Long senderId);

    /** viewer가 아직 읽지 않은, 남이 보낸 그룹 메시지 전체 (읽음 처리·말풍선 집계용) */
    @Query("""
            select m from Message m
            where m.group.id = :groupId
              and m.sender.id <> :viewerId
              and not exists (
                  select 1 from MessageRead r
                  where r.message = m and r.reader.id = :viewerId
              )
            order by m.id asc
            """)
    List<Message> findUnreadInGroup(@Param("groupId") Long groupId, @Param("viewerId") Long viewerId);

    /** viewer가 아직 읽지 않은, 특정 멤버가 보낸 메시지 (확대 뷰용) */
    @Query("""
            select m from Message m
            where m.sender.id = :senderId
              and m.sender.id <> :viewerId
              and not exists (
                  select 1 from MessageRead r
                  where r.message = m and r.reader.id = :viewerId
              )
            order by m.id asc
            """)
    List<Message> findUnreadBySender(@Param("senderId") Long senderId, @Param("viewerId") Long viewerId);
}
