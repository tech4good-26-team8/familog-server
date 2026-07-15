package com.familog.server.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.familog.server.domain.GenerationStatus;
import com.familog.server.domain.Member;
import com.familog.server.domain.Message;
import com.familog.server.domain.MessageType;

/** 메인 격자 카드: 아바타 + 말풍선(안읽은 메시지) 데이터 */
public record MemberCardResponse(
        Long memberId,
        String name,
        String avatarUrl,
        GenerationStatus avatarStatus,
        int unreadCount,
        UnreadPreview latestUnread
) {

    public static MemberCardResponse of(Member member, List<Message> unreadMessages) {
        UnreadPreview preview = unreadMessages.isEmpty()
                ? null
                : UnreadPreview.from(unreadMessages.get(unreadMessages.size() - 1));
        return new MemberCardResponse(
                member.getId(),
                member.getName(),
                member.getAvatarUrl(),
                member.getAvatarStatus(),
                unreadMessages.size(),
                preview
        );
    }

    public record UnreadPreview(Long messageId, MessageType type, String textPreview, LocalDateTime createdAt) {

        private static final int PREVIEW_LENGTH = 30;

        public static UnreadPreview from(Message message) {
            String text = message.getText();
            String preview = (text != null && text.length() > PREVIEW_LENGTH)
                    ? text.substring(0, PREVIEW_LENGTH) + "…"
                    : text;
            return new UnreadPreview(message.getId(), message.getType(), preview, message.getCreatedAt());
        }
    }
}
