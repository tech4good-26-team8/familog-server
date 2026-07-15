package com.familog.server.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.familog.server.config.FamilogProperties;
import com.familog.server.service.FileStorageService;

/**
 * familog-ai(:8000) 실제 호출 구현. familog.ai-mock=false 로 활성화.
 * 계약은 03 API 명세 familog-ai 섹션 (필드 snake_case).
 */
@Component
@ConditionalOnProperty(name = "familog.ai-mock", havingValue = "false")
public class RealAiClient implements AiClient {

    private final RestClient restClient;
    private final FileStorageService fileStorageService;

    public RealAiClient(FamilogProperties properties, FileStorageService fileStorageService) {
        // ai 서버가 멈춰도 워커가 무한 대기하지 않도록 타임아웃 필수 (초과 시 FAILED 전이)
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(180));
        this.restClient = RestClient.builder()
                .baseUrl(properties.aiBaseUrl())
                .requestFactory(requestFactory)
                .build();
        this.fileStorageService = fileStorageService;
    }

    record AvatarResponse(String avatar_path) {}
    record VoicepackResponse(String voicepack_id) {}
    record TtsResponse(String audio_path) {}
    record SttResponse(String text) {}

    @Override
    public String generateAvatar(Long memberId, String sourceRelativePath) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("photo", new FileSystemResource(fileStorageService.resolve(sourceRelativePath)));
        body.add("member_id", String.valueOf(memberId));
        AvatarResponse response = restClient.post().uri("/avatar")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(AvatarResponse.class);
        return response.avatar_path();
    }

    @Override
    public String registerVoicepack(Long memberId, String referenceRelativePath, String script) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audio", new FileSystemResource(fileStorageService.resolve(referenceRelativePath)));
        body.add("script", script);
        body.add("member_id", String.valueOf(memberId));
        VoicepackResponse response = restClient.post().uri("/voicepack")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(VoicepackResponse.class);
        return response.voicepack_id();
    }

    @Override
    public String synthesizeTts(String text, String voicepackId, String outputRelativePath) {
        TtsResponse response = restClient.post().uri("/tts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("text", text, "voicepack_id", voicepackId, "output_name", outputRelativePath))
                .retrieve()
                .body(TtsResponse.class);
        return response.audio_path();
    }

    @Override
    public String transcribeStt(String audioRelativePath) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audio", new FileSystemResource(fileStorageService.resolve(audioRelativePath)));
        SttResponse response = restClient.post().uri("/stt")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(SttResponse.class);
        return response.text();
    }
}
