package com.familog.server.dto.response;

import com.familog.server.domain.GenerationStatus;
import com.familog.server.domain.Member;

public record JoinMemberResponse(
        Long memberId,
        Long groupId,
        String voiceScript,
        GenerationStatus avatarStatus,
        GenerationStatus voiceStatus
) {

    public static JoinMemberResponse of(Member member, String voiceScript) {
        return new JoinMemberResponse(
                member.getId(), member.getGroup().getId(), voiceScript,
                member.getAvatarStatus(), member.getVoiceStatus());
    }
}
