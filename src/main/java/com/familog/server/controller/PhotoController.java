package com.familog.server.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.familog.server.dto.response.PhotoResponse;
import com.familog.server.service.PhotoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "photo", description = "추억 기록 갤러리 (보관함)")
@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    @Operation(summary = "사진 업로드", description = "위치 텍스트·촬영일 선택 입력 — 촬영일 미입력 시 KST 업로드 날짜로 저장. AI 변환 없음.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PhotoResponse upload(@RequestParam Long uploaderId,
                                @RequestPart MultipartFile image,
                                @RequestParam(required = false) String location,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate takenDate) {
        return photoService.upload(uploaderId, image, location, takenDate);
    }

    @Operation(summary = "갤러리 목록", description = "직접 업로드 사진 + 채팅 이미지 자동 수집 병합. KST 날짜 필터(캘린더 선택), date 없으면 전체 최신순. 채팅 유래 항목은 photoId가 음수(메시지 id의 음수)이며 location 없음.")
    @GetMapping
    public List<PhotoResponse> list(@RequestParam Long groupId,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return photoService.list(groupId, date);
    }
}
