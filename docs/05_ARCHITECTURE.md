# 아키텍처

시스템 **구조** 문서 — 무엇이 어디에 있고, 각 부분의 책임이 무엇인지.
흐름·순서·"왜"는 `02_TECH_FLOW.md`, API 목록은 `03_API_SPEC.md` 담당.

---

## 1. 전체 구성도

```
                        ┌─ MySQL (도메인 데이터: 경로·상태만)
[프론트] ──REST──▶ [familog-server :8080] ──HTTP──▶ [familog-ai :8000]
   ▲                Java 17 / Spring Boot            Python / FastAPI
   │                도메인·상태관리·저장·파일서빙        AI 생성 전담
   │                                                  ├─ CosyVoice2 (로컬, TTS/보이스팩)
   └── /files/** (정적 파일) ◀── 공유 데이터 디렉토리 ──┘  ├─ OpenAI 이미지 API (외부, 아바타)
                                                       └─ openai-whisper (로컬, STT)
```

- 전부 localhost (한 대의 MacBook M2). 배포 없음.
- 프론트 ↔ ai 직접 통신 없음. 항상 server 경유.

## 2. 책임 분리

| | familog-server | familog-ai |
|---|---|---|
| 역할 | 관리자: 도메인·상태·저장·제공 | 생성 공장: 입력 → 결과물 경로 |
| 상태(PENDING→READY) 관리 | O (소유) | X (무상태, 요청-응답만) |
| DB 접근 | O (MySQL) | X |
| 파일 | 경로를 DB에 저장, `/files/**`로 서빙 | 생성 파일을 데이터 디렉토리에 쓰기 |
| 실패 처리 | FAILED 전이 + 폴백은 프론트 몫 | 500 반환하면 끝 |

**ai 서버는 무상태(stateless).** 상태·재시도·폴백 판단은 전부 server가 가진다. 그래서 ai 내부 모델을 갈아끼워도 server는 안 바뀐다.

## 3. familog-server 패키지 구조

```
com.familog.server
├── controller/     # REST 엔드포인트. 검증·DTO 변환만, 로직 없음
├── service/        # 비즈니스 로직 + 비동기 생성 오케스트레이션(상태 전이)
├── client/         # AiClient 인터페이스 + MockAiClient / RealAiClient
├── repository/     # Spring Data JPA
├── domain/         # 엔티티(FamilyGroup, Member, Message, Photo) + GenerationStatus enum
├── dto/            # ~Request / ~Response record
└── config/         # @Async 설정, 정적 리소스(/files) 매핑, AI 서버 URL 프로퍼티
```

핵심 구조 포인트:
- **AiClient는 인터페이스.** 백엔드는 `MockAiClient`(잠깐 뒤 READY + 더미 경로)로 전체 흐름을 먼저 완성하고, ai 서버가 준비되면 `RealAiClient`로 교체 (02 §5-1). 교체는 프로퍼티 스위치 하나로.
- 생성 서비스 패턴 (아바타·보이스팩·TTS·STT 공통):
  ```
  저장(PENDING) → 즉시 반환
  @Async { PROCESSING → AiClient 호출 → 성공: 경로 저장+READY / 실패: FAILED }
  ```

## 4. familog-ai 디렉토리 구조

```
familog-ai/
├── main.py          # FastAPI 앱 생성, 라우터 등록, /health
├── routers/         # avatar.py, voice.py(voicepack+tts), stt.py — 엔드포인트만
├── services/        # openai_avatar.py, cosyvoice_tts.py, stt.py — 모델 추론 로직
├── core/
│   └── config.py    # .env 로딩 (OPENAI_API_KEY, 데이터 경로 등)
├── data/            # 생성 결과물 (gitignore)
└── requirements.txt
```

- 무거운 모델(CosyVoice2)은 **프로세스 시작 시 1회 로드**해서 재사용 (요청마다 로드 금지 — 16GB 메모리).

## 5. ERD

`06_ERD.md`로 분리. 테이블 4개(family_group / member / message / photo)와 관계·컬럼 정의는 그 문서가 단일 출처.

## 6. 파일 저장 레이아웃

같은 머신이므로 **공유 데이터 디렉토리** 하나를 두 서버가 함께 쓴다.
ai가 쓰고(write), server가 서빙(read)한다.

```
~/familog-data/            # 위치는 양쪽 설정값으로 통일 (하드코딩 금지)
├── avatars/{memberId}.png       # ai 생성 (3D 캐릭터)
├── voicepacks/{memberId}/       # ai 저장 (온보딩 낭독 참조 오디오 + 대사)
├── greetings/{memberId}.wav     # ai 생성 (보이스팩 인사말 샘플 TTS)
├── messages/{messageId}.wav     # TEXT→TTS 생성(ai) 또는 VOICE 원본(server 저장)
├── messages/{messageId}.jpg     # IMAGE 메시지 (server 저장)
└── photos/{photoId}.jpg         # server 저장 (갤러리 사진)
```

- server는 이 디렉토리를 `/files/**` 정적 경로로 매핑 → 프론트는 URL로 바로 로드.
- DB의 `*_url` 컬럼에는 `/files/...` 형태의 상대 경로를 저장.
- ai 응답의 `*_path`는 이 디렉토리 기준 경로 → server가 `/files/...`로 변환해 저장.

## 7. 비동기 처리 구조 (server)

- `@EnableAsync` + 전용 `ThreadPoolTaskExecutor` (풀 크기 작게 — 로컬 데모, 동시 생성 몇 건이면 충분).
- 생성 트리거는 온보딩 단계별 독립: 얼굴 스캔 → 아바타 @Async, 낭독 → 보이스팩 @Async, 메시지 전송 → TTS/STT @Async.
- 타임아웃: AI 호출 클라이언트에 설정 (아바타 ~60s, TTS/STT ~120s 여유) → 초과 시 FAILED.
- 채팅 실시간성은 프론트 폴링(`GET /api/messages?after=`)으로. WebSocket은 하루 범위에 과함.
