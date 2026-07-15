package com.familog.server.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.familog.server.dto.request.ReadMessagesRequest;
import com.familog.server.dto.request.SendTextMessageRequest;
import com.familog.server.dto.response.MessageResponse;
import com.familog.server.dto.response.ReadResultResponse;
import com.familog.server.service.MessageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "message", description = "가족 채팅 (텍스트→TTS, 녹음→STT, 이미지)")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "텍스트 메시지 전송", description = "즉시 응답, 백그라운드에서 보낸이 보이스팩으로 TTS 생성 → audioUrl이 채워진다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendText(@Valid @RequestBody SendTextMessageRequest request) {
        return messageService.sendText(request);
    }

    @Operation(summary = "음성 메시지 전송", description = "녹음 원본 그대로 저장(재합성 안 함). 즉시 응답, 백그라운드 STT로 text(자막)가 채워진다.")
    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendVoice(@RequestParam Long senderId, @RequestPart MultipartFile audio) {
        return messageService.sendVoice(senderId, audio);
    }

    @Operation(summary = "이미지 메시지 전송", description = "AI 변환 없음 — 동기 저장, 즉시 완결.")
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendImage(@RequestParam Long senderId, @RequestPart MultipartFile image) {
        return messageService.sendImage(senderId, image);
    }

    @Operation(summary = "메시지 목록 (폴링 겸)", description = "after 이후 메시지만 반환. 프론트가 1~2초 폴링해 실시간성 확보.")
    @GetMapping
    public List<MessageResponse> list(@RequestParam Long groupId, @RequestParam(required = false) Long after) {
        return messageService.list(groupId, after);
    }

    @Operation(summary = "읽음 처리", description = "확대 뷰 = {readerId, senderId} → 그 멤버 것만 / 채팅 진입 = {readerId} → 그룹 전체. 멱등.")
    @PostMapping("/read")
    public ReadResultResponse markRead(@Valid @RequestBody ReadMessagesRequest request) {
        return messageService.markRead(request);
    }
}
