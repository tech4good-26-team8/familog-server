package com.familog.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.familog.server.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByGroupIdOrderByIdAsc(Long groupId);
}
