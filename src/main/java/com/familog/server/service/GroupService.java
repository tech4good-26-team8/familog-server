package com.familog.server.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.familog.server.domain.FamilyGroup;
import com.familog.server.dto.request.CreateGroupRequest;
import com.familog.server.dto.response.GroupResponse;
import com.familog.server.exception.BusinessException;
import com.familog.server.exception.ErrorCode;
import com.familog.server.repository.FamilyGroupRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final FamilyGroupRepository familyGroupRepository;

    @Transactional
    public GroupResponse create(CreateGroupRequest request) {
        FamilyGroup group = FamilyGroup.builder()
                .name(request.name())
                .inviteCode(generateUniqueInviteCode())
                .build();
        return GroupResponse.from(familyGroupRepository.save(group));
    }

    public GroupResponse get(Long groupId) {
        FamilyGroup group = familyGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));
        return GroupResponse.from(group);
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = String.format("%06d", RANDOM.nextInt(1_000_000));
        } while (familyGroupRepository.existsByInviteCode(code));
        return code;
    }
}
