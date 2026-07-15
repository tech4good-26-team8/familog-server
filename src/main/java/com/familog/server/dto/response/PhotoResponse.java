package com.familog.server.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.familog.server.domain.Message;
import com.familog.server.domain.Photo;

public record PhotoResponse(
        Long photoId,
        Long uploaderId,
        String uploaderName,
        String imageUrl,
        String location,
        LocalDate takenDate,
        LocalDateTime createdAt
) {

    public static PhotoResponse from(Photo photo) {
        return new PhotoResponse(
                photo.getId(),
                photo.getUploader().getId(),
                photo.getUploader().getName(),
                photo.getImageUrl(),
                photo.getLocation(),
                photo.getTakenDate(),
                photo.getCreatedAt()
        );
    }

    /** 채팅 IMAGE 메시지 → 갤러리 항목. photoId는 photo PK와 충돌하지 않도록 메시지 id의 음수 */
    public static PhotoResponse fromMessage(Message message) {
        return new PhotoResponse(
                -message.getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getImageUrl(),
                null,
                message.getCreatedAt().toLocalDate(),
                message.getCreatedAt()
        );
    }
}
