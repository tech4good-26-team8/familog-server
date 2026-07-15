package com.familog.server.service;

import java.util.function.Consumer;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.familog.server.client.AiClient;
import com.familog.server.config.FamilogProperties;
import com.familog.server.domain.GenerationStatus;
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
 *
 * 엔티티 갱신은 항상 "재조회 → 필드 변경 → 즉시 저장"으로 짧게 끊는다.
 * 긴 AI 호출 동안 들고 있던 엔티티를 통째로 save하면
 * 동시에 끝난 다른 작업의 결과(아바타 ↔ 보이스팩)를 덮어쓴다(lost update).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    private static final int MESSAGE_TEXT_LIMIT = 500;
    private static final long VOICEPACK_WAIT_INTERVAL_MILLIS = 2000L;
    private static final int VOICEPACK_WAIT_MAX_TRIES = 30;

    private final AiClient aiClient;
    private final MemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final FileStorageService fileStorageService;
    private final FamilogProperties properties;

    @Async("generationExecutor")
    public void generateAvatar(Long memberId, String sourceRelativePath) {
        updateMember(memberId, Member::markAvatarProcessing);
        try {
            String avatarPath = aiClient.generateAvatar(memberId, sourceRelativePath);
            String avatarUrl = fileStorageService.toFileUrl(avatarPath);
            updateMember(memberId, member -> member.markAvatarReady(avatarUrl));
        } catch (Exception e) {
            log.error("아바타 생성 실패: member={}", memberId, e);
            updateMember(memberId, Member::markAvatarFailed);
        }
    }

    @Async("generationExecutor")
    public void registerVoicepack(Long memberId, String referenceRelativePath, String script) {
        updateMember(memberId, Member::markVoiceProcessing);
        try {
            String voicepackId = aiClient.registerVoicepack(memberId, referenceRelativePath, script);
            updateMember(memberId, member -> member.markVoiceReady(voicepackId));
            generateGreeting(memberId, voicepackId);
        } catch (Exception e) {
            log.error("보이스팩 등록 실패: member={}", memberId, e);
            updateMember(memberId, Member::markVoiceFailed);
        }
    }

    /** 보이스팩 READY 직후 인사말 샘플 TTS 자동 생성 (1-5 생성 완료 화면용) */
    private void generateGreeting(Long memberId, String voicepackId) {
        try {
            String memberName = memberRepository.findById(memberId).orElseThrow().getName();
            String greetingPath = aiClient.synthesizeTts(
                    properties.greetingFor(memberName), voicepackId, "greetings/" + memberId + ".wav");
            String greetingUrl = fileStorageService.toFileUrl(greetingPath);
            updateMember(memberId, member -> member.attachGreetingAudio(greetingUrl));
        } catch (Exception e) {
            log.error("인사말 생성 실패: member={} — 화면은 인사말 없이 진행", memberId, e);
        }
    }

    @Async("generationExecutor")
    public void generateMessageTts(Long messageId) {
        updateMessage(messageId, Message::markProcessing);
        try {
            Message message = messageRepository.findById(messageId).orElseThrow();
            String voicepackId = waitForVoicepack(message.getSender().getId());
            String audioPath = aiClient.synthesizeTts(message.getText(), voicepackId, "messages/" + messageId + ".wav");
            String audioUrl = fileStorageService.toFileUrl(audioPath);
            updateMessage(messageId, m -> m.completeTts(audioUrl));
        } catch (Exception e) {
            log.error("메시지 TTS 실패: message={}", messageId, e);
            updateMessage(messageId, Message::markFailed);
        }
    }

    /** 가입 직후 첫 메시지 케이스: 보이스팩 등록이 아직 진행 중이면 최대 60초 기다린다 */
    private String waitForVoicepack(Long senderId) throws InterruptedException {
        for (int i = 0; i < VOICEPACK_WAIT_MAX_TRIES; i++) {
            Member sender = memberRepository.findById(senderId).orElseThrow();
            if (sender.getVoicepackId() != null) {
                return sender.getVoicepackId();
            }
            if (sender.getVoiceStatus() == GenerationStatus.FAILED) {
                break;
            }
            Thread.sleep(VOICEPACK_WAIT_INTERVAL_MILLIS);
        }
        throw new IllegalStateException("보이스팩 미등록(sender=" + senderId + ") — 표준 TTS 폴백은 프론트 처리");
    }

    @Async("generationExecutor")
    public void transcribeMessageVoice(Long messageId, String audioRelativePath) {
        updateMessage(messageId, Message::markProcessing);
        try {
            String text = aiClient.transcribeStt(audioRelativePath);
            String trimmed = text.length() > MESSAGE_TEXT_LIMIT ? text.substring(0, MESSAGE_TEXT_LIMIT) : text;
            updateMessage(messageId, m -> m.completeStt(trimmed));
        } catch (Exception e) {
            log.error("메시지 STT 실패: message={}", messageId, e);
            updateMessage(messageId, Message::markFailed);
        }
    }

    private void updateMember(Long memberId, Consumer<Member> updater) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        updater.accept(member);
        memberRepository.save(member);
    }

    private void updateMessage(Long messageId, Consumer<Message> updater) {
        Message message = messageRepository.findById(messageId).orElseThrow();
        updater.accept(message);
        messageRepository.save(message);
    }
}
