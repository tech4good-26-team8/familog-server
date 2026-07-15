package com.familog.server.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.familog.server.service.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 서버 없이 전체 흐름을 완성하기 위한 mock (02 §5-1 병렬 원칙).
 * - 아바타: 업로드한 원본 사진을 그대로 아바타로 반환 (화면에 실제로 뜨도록)
 * - TTS: 참조 낭독 오디오를 복사해 반환 (실제로 재생되도록)
 * - STT: 고정 mock 텍스트 반환
 * 실패 시에는 예외를 그대로 던진다 — 죽은 URL을 READY로 표시하는 것보다 FAILED가 맞다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "familog.ai-mock", havingValue = "true", matchIfMissing = true)
public class MockAiClient implements AiClient {

    private static final long MOCK_DELAY_MILLIS = 2000L;

    private final FileStorageService fileStorageService;

    @Override
    public String generateAvatar(Long memberId, String sourceRelativePath) {
        simulateDelay();
        log.info("[mock] 아바타 생성: member={} → 원본 사진 그대로 반환", memberId);
        return sourceRelativePath;
    }

    @Override
    public String registerVoicepack(Long memberId, String referenceRelativePath, String script) {
        simulateDelay();
        log.info("[mock] 보이스팩 등록: member={}", memberId);
        return "vp_" + memberId;
    }

    @Override
    public String synthesizeTts(String text, String voicepackId, String outputRelativePath) {
        simulateDelay();
        String memberId = voicepackId.replace("vp_", "");
        String reference = findReference(memberId);
        fileStorageService.copy(reference, outputRelativePath);
        log.info("[mock] TTS 생성: {} 복사 → {}", reference, outputRelativePath);
        return outputRelativePath;
    }

    @Override
    public String transcribeStt(String audioRelativePath) {
        simulateDelay();
        return "(mock) 음성 메시지 텍스트 변환 결과입니다.";
    }

    /** 확장자를 가리지 않고 reference.* 참조 오디오를 찾는다 (m4a/aac 녹음 대응) */
    private String findReference(String memberId) {
        Path dir = fileStorageService.resolve("voicepacks/" + memberId);
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(path -> path.getFileName().toString().startsWith("reference."))
                    .findFirst()
                    .map(path -> "voicepacks/" + memberId + "/" + path.getFileName())
                    .orElseThrow(() -> new IllegalStateException("참조 오디오 없음: member=" + memberId));
        } catch (IOException e) {
            throw new IllegalStateException("보이스팩 디렉토리 없음: member=" + memberId, e);
        }
    }

    private void simulateDelay() {
        try {
            Thread.sleep(MOCK_DELAY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
