package com.familog.server.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
}
