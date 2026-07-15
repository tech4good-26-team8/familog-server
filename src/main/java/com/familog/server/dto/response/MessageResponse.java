package com.familog.server.dto.response;

import java.time.LocalDateTime;

import com.familog.server.domain.GenerationStatus;
import com.familog.server.domain.Message;
import com.familog.server.domain.MessageType;

public record MessageResponse(
        Long messageId,
        Long senderId,
        String senderName,
        MessageType type,
        String text,
        String audioUrl,
        String imageUrl,
        GenerationStatus convertStatus,
        LocalDateTime createdAt
) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getType(),
                message.getText(),
                message.getAudioUrl(),
                message.getImageUrl(),
                message.getConvertStatus(),
                message.getCreatedAt()
        );
    }
}
