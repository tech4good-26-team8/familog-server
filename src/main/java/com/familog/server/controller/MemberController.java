package com.familog.server.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.familog.server.dto.request.JoinMemberRequest;
import com.familog.server.dto.request.UpdateMemberRequest;
import com.familog.server.dto.response.JoinMemberResponse;
import com.familog.server.dto.response.MemberCardResponse;
import com.familog.server.dto.response.MemberResponse;
import com.familog.server.dto.response.MessageResponse;
import com.familog.server.service.MemberService;
import com.familog.server.service.MessageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "member", description = "온보딩(가입·아바타·보이스팩) / 메인 격자 / 프로필")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MessageService messageService;

    @Operation(summary = "그룹 참여 (가입)", description = "이름+초대코드로 가입. 즉시 memberId와 낭독 지정 문장(voiceScript)을 응답한다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JoinMemberResponse join(@Valid @RequestBody JoinMemberRequest request) {
        return memberService.join(request);
    }

    @Operation(summary = "아바타 생성 (사진 업로드)", description = "얼굴 사진 업로드 — 촬영/앨범 동일 계약. 즉시 응답 후 백그라운드 생성, 상태는 멤버 단건 폴링으로 확인.")
    @PostMapping(value = "/{memberId}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MemberResponse uploadAvatar(@PathVariable Long memberId, @RequestPart MultipartFile photo) {
        return memberService.uploadAvatar(memberId, photo);
    }

    @Operation(summary = "보이스팩 등록 (낭독)", description = "지정 문장 낭독 녹음 업로드. 즉시 응답 후 백그라운드 등록, READY 되면 인사말 샘플 TTS 자동 생성.")
    @PostMapping(value = "/{memberId}/voicepack", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MemberResponse uploadVoicepack(@PathVariable Long memberId, @RequestPart MultipartFile audio) {
        return memberService.uploadVoicepack(memberId, audio);
    }

    @Operation(summary = "멤버 단건 (폴링·생성 완료 화면)", description = "avatarStatus·voiceStatus 폴링 + avatarUrl·greetingAudioUrl(내 목소리 인사말 샘플).")
    @GetMapping("/{memberId}")
    public MemberResponse get(@PathVariable Long memberId) {
        return memberService.get(memberId);
    }

    @Operation(summary = "멤버 목록 (메인 격자)", description = "그룹 멤버 아바타+이름 + viewerId 기준 말풍선 데이터(안읽은 수·최신 안읽은 미리보기).")
    @GetMapping
    public List<MemberCardResponse> list(@RequestParam Long groupId, @RequestParam(required = false) Long viewerId) {
        return memberService.listWithBubbles(groupId, viewerId);
    }

    @Operation(summary = "멤버 확대 뷰 메시지", description = "아바타 클릭 시: 그 멤버가 보낸 안읽은 메시지 원본(오래된 순) — 프론트가 자동 재생. unreadOnly=false면 전체 이력.")
    @GetMapping("/{memberId}/messages")
    public List<MessageResponse> messagesBySender(@PathVariable Long memberId,
                                                  @RequestParam Long viewerId,
                                                  @RequestParam(defaultValue = "true") boolean unreadOnly) {
        return messageService.listBySender(memberId, viewerId, unreadOnly);
    }

    @Operation(summary = "프로필 수정", description = "이름 변경. 사진 변경은 아바타 API 재호출.")
    @PatchMapping("/{memberId}")
    public MemberResponse update(@PathVariable Long memberId, @Valid @RequestBody UpdateMemberRequest request) {
        return memberService.update(memberId, request);
    }
}
