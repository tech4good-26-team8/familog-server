package com.familog.server.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.familog.server.config.FamilogProperties;
import com.familog.server.domain.FamilyGroup;
import com.familog.server.domain.Member;
import com.familog.server.domain.Message;
import com.familog.server.dto.request.JoinMemberRequest;
import com.familog.server.dto.request.UpdateMemberRequest;
import com.familog.server.dto.response.JoinMemberResponse;
import com.familog.server.dto.response.MemberCardResponse;
import com.familog.server.dto.response.MemberResponse;
import com.familog.server.exception.BusinessException;
import com.familog.server.exception.ErrorCode;
import com.familog.server.repository.FamilyGroupRepository;
import com.familog.server.repository.MemberRepository;
import com.familog.server.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final FamilyGroupRepository familyGroupRepository;
    private final MessageRepository messageRepository;
    private final FileStorageService fileStorageService;
    private final GenerationService generationService;
    private final FamilogProperties properties;

    @Transactional
    public JoinMemberResponse join(JoinMemberRequest request) {
        FamilyGroup group = familyGroupRepository.findByInviteCode(request.inviteCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_CODE_NOT_FOUND));
        Member member = memberRepository.save(Member.builder()
                .group(group)
                .name(request.name())
                .build());
        return JoinMemberResponse.of(member, properties.voiceScriptFor(member.getName()));
    }

    /** 얼굴 사진 업로드 → 즉시 응답, 백그라운드 아바타 생성 */
    @Transactional
    public MemberResponse uploadAvatar(Long memberId, MultipartFile photo) {
        Member member = findMember(memberId);
        String ext = fileStorageService.extensionOf(photo, "jpg");
        String sourcePath = fileStorageService.store(photo, "uploads/selfies/" + memberId + "." + ext);
        generationService.generateAvatar(memberId, sourcePath);
        return MemberResponse.from(member);
    }

    /** 지정 문장 낭독 업로드 → 즉시 응답, 백그라운드 보이스팩 등록 + 인사말 생성 */
    @Transactional
    public MemberResponse uploadVoicepack(Long memberId, MultipartFile audio) {
        Member member = findMember(memberId);
        String ext = fileStorageService.extensionOf(audio, "wav");
        String referencePath = fileStorageService.store(audio, "voicepacks/" + memberId + "/reference." + ext);
        generationService.registerVoicepack(memberId, referencePath, properties.voiceScriptFor(member.getName()));
        return MemberResponse.from(member);
    }

    public MemberResponse get(Long memberId) {
        return MemberResponse.from(findMember(memberId));
    }

    /** 메인 격자: 멤버 + viewer 기준 말풍선(안읽은 메시지) 데이터 */
    public List<MemberCardResponse> listWithBubbles(Long groupId, Long viewerId) {
        List<Member> members = memberRepository.findByGroupIdOrderByIdAsc(groupId);
        Map<Long, List<Message>> unreadBySender = (viewerId == null)
                ? Map.of()
                : messageRepository.findUnreadInGroup(groupId, viewerId).stream()
                        .collect(Collectors.groupingBy(message -> message.getSender().getId()));
        return members.stream()
                .map(member -> MemberCardResponse.of(member, unreadBySender.getOrDefault(member.getId(), List.of())))
                .toList();
    }

    @Transactional
    public MemberResponse update(Long memberId, UpdateMemberRequest request) {
        Member member = findMember(memberId);
        member.rename(request.name());
        return MemberResponse.from(member);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
