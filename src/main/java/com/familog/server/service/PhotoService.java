package com.familog.server.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.familog.server.domain.Member;
import com.familog.server.domain.Photo;
import com.familog.server.dto.response.PhotoResponse;
import com.familog.server.exception.BusinessException;
import com.familog.server.exception.ErrorCode;
import com.familog.server.repository.MemberRepository;
import com.familog.server.repository.PhotoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public PhotoResponse upload(Long uploaderId, MultipartFile image, String location, LocalDate takenDate) {
        Member uploader = memberRepository.findById(uploaderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        String ext = fileStorageService.extensionOf(image, "jpg");
        String imagePath = fileStorageService.store(image, "photos/" + UUID.randomUUID() + "." + ext);
        Photo photo = photoRepository.save(Photo.builder()
                .group(uploader.getGroup())
                .uploader(uploader)
                .imageUrl(fileStorageService.toFileUrl(imagePath))
                .location(location)
                .takenDate(takenDate != null ? takenDate : LocalDate.now())
                .build());
        return PhotoResponse.from(photo);
    }

    public List<PhotoResponse> list(Long groupId, LocalDate date) {
        List<Photo> photos = (date == null)
                ? photoRepository.findByGroupIdOrderByCreatedAtDesc(groupId)
                : photoRepository.findByGroupIdAndTakenDateOrderByCreatedAtDesc(groupId, date);
        return photos.stream().map(PhotoResponse::from).toList();
    }
}
