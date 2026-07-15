package com.familog.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI familogOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Familog API")
                .description("""
                        패밀로그 — AI 음성과 3D 캐릭터로 가족의 소통과 추억을 기록하는 패밀리 커뮤니케이션 플랫폼.

                        - 인증 없음(로컬 데모): 가입 시 받은 memberId를 클라이언트가 저장해 사용
                        - 생성물(아바타·보이스팩·TTS·STT)은 전부 비동기 — 즉시 응답 후 상태 폴링 (PENDING → PROCESSING → READY / FAILED)
                        - 파일(이미지·오디오)은 응답의 `/files/**` URL로 바로 로드
                        - 전체 API 지도·우선순위: docs/03_API_SPEC.md""")
                .version("v1"));
    }
}
