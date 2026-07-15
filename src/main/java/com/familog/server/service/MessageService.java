package com.familog.server.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.familog.server.domain.GenerationStatus;
import com.familog.server.domain.Member;
import com.familog.server.domain.Message;
import com.familog.server.domain.MessageType;
import com.familog.server.dto.request.ReadMessagesRequest;
import com.familog.server.dto.request.SendTextMessageRequest;
import com.familog.server.dto.response.MessageResponse;
import com.familog.server.dto.response.ReadResultResponse;
import com.familog.server.exception.BusinessException;
import com.familog.server.exception.ErrorCode;
import com.familog.server.repository.MemberRepository;
import com.familog.server.repository.MessageReadRepository;
import com.familog.server.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageReadRepository messageReadRepository;
    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;
    private final GenerationService generationService;

    /** 텍스트 메시지: 즉시 저장·응답 → 백그라운드 TTS(보낸이 보이스팩) */
    @Transactional
    public MessageResponse sendText(SendTextMessageRequest request) {
        Member sender = findMember(request.senderId());
        Message message = messageRepository.save(Message.builder()
                .group(sender.getGroup())
                .sender(sender)
                .type(MessageType.TEXT)
                .text(request.text())
                .convertStatus(GenerationStatus.PENDING)
                .build());
        dispatchAfterCommit(() -> generationService.generateMessageTts(message.getId()));
        return MessageResponse.from(message);
    }

    /** 음성 메시지: 원본 그대로 저장(02 §8-7) → 백그라운드 STT */
    @Transactional
    public MessageResponse sendVoice(Long senderId, MultipartFile audio) {
        Member sender = findMember(senderId);
        Message message = messageRepository.save(Message.builder()
                .group(sender.getGroup())
                .sender(sender)
                .type(MessageType.VOICE)
                .convertStatus(GenerationStatus.PENDING)
                .build());
        String ext = fileStorageService.extensionOf(audio, "wav");
        String audioPath = fileStorageService.store(audio, "messages/" + message.getId() + "." + ext);
        message.attachVoiceAudio(fileStorageService.toFileUrl(audioPath));
        dispatchAfterCommit(() -> generationService.transcribeMessageVoice(message.getId(), audioPath));
        return MessageResponse.from(message);
    }

    /** 이미지 메시지: AI 변환 없음 — 동기 저장, 즉시 완결 */
    @Transactional
    public MessageResponse sendImage(Long senderId, MultipartFile image) {
        Member sender = findMember(senderId);
        Message message = messageRepository.save(Message.builder()
                .group(sender.getGroup())
                .sender(sender)
                .type(MessageType.IMAGE)
                .convertStatus(GenerationStatus.READY)
                .build());
        String ext = fileStorageService.extensionOf(image, "jpg");
        String imagePath = fileStorageService.store(image, "messages/" + message.getId() + "." + ext);
        message.attachImage(fileStorageService.toFileUrl(imagePath));
        return MessageResponse.from(message);
    }

    /** 채팅 목록 (after 이후만 — 프론트 폴링용) */
    public List<MessageResponse> list(Long groupId, Long after) {
        List<Message> messages = (after == null)
                ? messageRepository.findByGroupIdOrderByIdAsc(groupId)
                : messageRepository.findByGroupIdAndIdGreaterThanOrderByIdAsc(groupId, after);
        return messages.stream().map(MessageResponse::from).toList();
    }

    /** 확대 뷰: 특정 멤버가 보낸 (안읽은) 메시지 */
    public List<MessageResponse> listBySender(Long senderId, Long viewerId, boolean unreadOnly) {
        List<Message> messages = unreadOnly
                ? messageRepository.findUnreadBySender(senderId, viewerId)
                : messageRepository.findBySenderIdOrderByIdAsc(senderId);
        return messages.stream().map(MessageResponse::from).toList();
    }

    /** 읽음 처리 (멱등): senderId 있으면 그 멤버 것만, 없으면 그룹 전체. INSERT IGNORE 한 방 쿼리 */
    @Transactional
    public ReadResultResponse markRead(ReadMessagesRequest request) {
        Member reader = findMember(request.readerId());
        int count = messageReadRepository.markAllRead(reader.getGroup().getId(), reader.getId(), request.senderId());
        return new ReadResultResponse(count);
    }

    /** @Async 작업은 커밋 후 디스패치 — 커밋 전 findById로 인한 NoSuchElement 레이스 방지 */
    private void dispatchAfterCommit(Runnable task) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
