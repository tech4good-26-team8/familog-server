package com.familog.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.familog.server.domain.MessageRead;

public interface MessageReadRepository extends JpaRepository<MessageRead, Long> {

    boolean existsByMessageIdAndReaderId(Long messageId, Long readerId);
}
