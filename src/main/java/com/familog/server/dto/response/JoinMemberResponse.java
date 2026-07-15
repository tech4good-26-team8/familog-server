package com.familog.server.dto.response;

import com.familog.server.domain.GenerationStatus;
import com.familog.server.domain.Member;

public record JoinMemberResponse(
        Long memberId,
        String voiceScript,
        GenerationStatus avatarStatus,
        GenerationStatus voiceStatus
) {

    public static JoinMemberResponse of(Member member, String voiceScript) {
        return new JoinMemberResponse(member.getId(), voiceScript, member.getAvatarStatus(), member.getVoiceStatus());
    }
}
