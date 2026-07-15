package com.familog.server.service;

import java.security.SecureRandom;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    /**
     * 초대코드 유일성은 유니크 제약 + 충돌 시 재발급으로 보장 (check-then-insert 레이스 없음).
     * 재시도가 가능하도록 각 save가 자체 트랜잭션을 타게 한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public GroupResponse create(CreateGroupRequest request) {
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                FamilyGroup group = FamilyGroup.builder()
                        .name(request.name())
                        .inviteCode(generateInviteCode())
                        .build();
                return GroupResponse.from(familyGroupRepository.save(group));
            } catch (DataIntegrityViolationException e) {
                // 초대코드 충돌 — 새 코드로 재시도
            }
        }
        throw new BusinessException(ErrorCode.INVITE_CODE_GENERATION_FAILED);
    }

    public GroupResponse get(Long groupId) {
        FamilyGroup group = familyGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));
        return GroupResponse.from(group);
    }

    private String generateInviteCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
