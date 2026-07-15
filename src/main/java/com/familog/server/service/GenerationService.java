package com.familog.server.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.familog.server.client.AiClient;
import com.familog.server.config.FamilogProperties;
import com.familog.server.domain.Member;
import com.familog.server.domain.Message;
import com.familog.server.repository.MemberRepository;
import com.familog.server.repository.MessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 비동기 생성 파이프라인 (02 §2 공통 패턴).
 * PENDING 저장·즉시 응답은 각 도메인 서비스가, 여기서는
 * PROCESSING 전이 → AI 호출 → READY/FAILED 전이를 백그라운드로 수행한다.
 * (@Async 자기호출 문제를 피하려고 도메인 서비스와 분리)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    private final AiClient aiClient;
    private final MemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final FileStorageService fileStorageService;
    private final FamilogProperties properties;

    @Async("generationExecutor")
    public void generateAvatar(Long memberId, String sourceRelativePath) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.markAvatarProcessing();
        memberRepository.save(member);
        try {
            String avatarPath = aiClient.generateAvatar(memberId, sourceRelativePath);
            member.markAvatarReady(fileStorageService.toFileUrl(avatarPath));
        } catch (Exception e) {
            log.error("아바타 생성 실패: member={}", memberId, e);
            member.markAvatarFailed();
        }
        memberRepository.save(member);
    }

    @Async("generationExecutor")
    public void registerVoicepack(Long memberId, String referenceRelativePath, String script) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.markVoiceProcessing();
        memberRepository.save(member);
        try {
            String voicepackId = aiClient.registerVoicepack(memberId, referenceRelativePath, script);
            member.markVoiceReady(voicepackId);
            memberRepository.save(member);
            generateGreeting(member, voicepackId);
        } catch (Exception e) {
            log.error("보이스팩 등록 실패: member={}", memberId, e);
            member.markVoiceFailed();
            memberRepository.save(member);
        }
    }

    /** 보이스팩 READY 직후 인사말 샘플 TTS 자동 생성 (1-5 생성 완료 화면용) */
    private void generateGreeting(Member member, String voicepackId) {
        try {
            String output = "greetings/" + member.getId() + ".wav";
            String greetingPath = aiClient.synthesizeTts(properties.greetingFor(member.getName()), voicepackId, output);
            member.attachGreetingAudio(fileStorageService.toFileUrl(greetingPath));
            memberRepository.save(member);
        } catch (Exception e) {
            log.error("인사말 생성 실패: member={} — 화면은 인사말 없이 진행", member.getId(), e);
        }
    }

    @Async("generationExecutor")
    public void generateMessageTts(Long messageId) {
        Message message = messageRepository.findById(messageId).orElseThrow();
        Member sender = memberRepository.findById(message.getSender().getId()).orElseThrow();
        message.markProcessing();
        messageRepository.save(message);
        try {
            if (sender.getVoicepackId() == null) {
                throw new IllegalStateException("보이스팩 미등록 - 표준 TTS 폴백은 프론트 처리");
            }
            String output = "messages/" + messageId + ".wav";
            String audioPath = aiClient.synthesizeTts(message.getText(), sender.getVoicepackId(), output);
            message.completeTts(fileStorageService.toFileUrl(audioPath));
        } catch (Exception e) {
            log.error("메시지 TTS 실패: message={}", messageId, e);
            message.markFailed();
        }
        messageRepository.save(message);
    }

    @Async("generationExecutor")
    public void transcribeMessageVoice(Long messageId, String audioRelativePath) {
        Message message = messageRepository.findById(messageId).orElseThrow();
        message.markProcessing();
        messageRepository.save(message);
        try {
            String text = aiClient.transcribeStt(audioRelativePath);
            message.completeStt(text);
        } catch (Exception e) {
            log.error("메시지 STT 실패: message={}", messageId, e);
            message.markFailed();
        }
        messageRepository.save(message);
    }
}
