package com.familog.server.client;

/**
 * familog-ai(:8000) 호출 계약. 구현체는 프로퍼티 familog.ai-mock 으로 교체한다 (05 아키텍처 §3).
 * 반환 경로는 전부 데이터 디렉토리 기준 상대경로.
 */
public interface AiClient {

    /** 얼굴 사진 → 3D풍 캐릭터 PNG 생성. 생성된 상대경로 반환 */
    String generateAvatar(Long memberId, String sourceRelativePath);

    /** 참조 오디오 + 대사 → 보이스팩 등록. voicepack 식별자 반환 */
    String registerVoicepack(Long memberId, String referenceRelativePath, String script);

    /** 텍스트 + 보이스팩 → 그 사람 목소리 낭독 WAV. 생성된 상대경로 반환 */
    String synthesizeTts(String text, String voicepackId, String outputRelativePath);

    /** 음성 → 텍스트 (STT) */
    String transcribeStt(String audioRelativePath);
}
