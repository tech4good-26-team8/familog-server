package com.familog.server.dto.response;

import com.familog.server.domain.GenerationStatus;
import com.familog.server.domain.Member;

public record MemberResponse(
        Long memberId,
        String name,
        String avatarUrl,
        GenerationStatus avatarStatus,
        GenerationStatus voiceStatus,
        String greetingAudioUrl
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getName(),
                member.getAvatarUrl(),
                member.getAvatarStatus(),
                member.getVoiceStatus(),
                member.getGreetingAudioUrl()
        );
    }
}
