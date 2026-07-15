package com.familog.server.dto.response;

import com.familog.server.domain.FamilyGroup;

public record GroupResponse(Long groupId, String name, String inviteCode) {

    public static GroupResponse from(FamilyGroup group) {
        return new GroupResponse(group.getId(), group.getName(), group.getInviteCode());
    }
}
