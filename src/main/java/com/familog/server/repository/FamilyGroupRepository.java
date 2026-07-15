package com.familog.server.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.familog.server.domain.FamilyGroup;

public interface FamilyGroupRepository extends JpaRepository<FamilyGroup, Long> {

    Optional<FamilyGroup> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
