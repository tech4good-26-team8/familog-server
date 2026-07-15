# API

엔드포인트 상위 인덱스. "어떤 모듈에 무슨 API가 있고, 어디까지 됐는지"를 기록하기 위함.

**역할 분담**

- 이 문서 = API 전체 지도 + 진행상황 (예정 포함). 한 줄 요약만.
- Swagger = 구현된 엔드포인트의 정확한 계약(요청/응답 스키마·예시). 상세는 여기서. (springdoc 붙이면 `/swagger-ui`)
- 02_TECH_FLOW.md = 플로우·비동기 패턴·기술 결정 기록. ERD는 05 §5.

**갱신 규칙**

- 엔드포인트를 추가/변경하는 커밋에서 이 표도 같이 수정한다(나중으로 미루지 않는다).
- 요청/응답 바디는 적지 않는다(Swagger 담당). 표는 가볍게 유지.
- 인증 없음(해커톤 로컬 데모). 클라이언트가 가입 때 받은 memberId를 저장해 갖고 다닌다. 프론트는 **ai 서버를 직접 호출하지 않는다** — 항상 server 경유.
- 생성물(아바타·보이스팩·TTS·STT)은 전부 비동기: 즉시 응답 후 상태 폴링. 상태값 = `PENDING / PROCESSING / READY / FAILED`.
- 상태: 예정 / WIP / 완료.

---

## familog-server (`:8080`, 프론트용)

**데모 최소셋** = 그룹 참여(가입), 아바타 생성+폴링, 보이스팩 등록, 멤버 격자(말풍선 포함), 확대 뷰+읽음 처리, 메시지 전송(텍스트·음성), 메시지 목록, 파일 서빙 — "아바타 탭 → 가족 목소리 자동 재생"이 시니어 소비의 핵심 데모.
갤러리는 2순위. 그룹 생성은 방 만들기 화면 확정으로 필요해졌으나 데모 리허설은 시드 그룹으로도 가능, 프로필 수정은 후순위. 메인 화면의 알림 종·Zzz(상태 표시)는 범위 미정 — API 없음.

### group — 가족 그룹 / 초대코드

| 기능 | Method | Path | 상태 | 설명 |
|---|---|---|---|---|
| 그룹 생성 (방 만들기) | POST | /api/groups | 완료 | 그룹명으로 생성, 6자리 고유 초대코드 자동 발급. 방장(첫 가족)은 발급된 코드로 아래 가입 API를 그대로 탄다 — 가입 경로 단일화. 방 만들기 화면 필요(와이어프레임 추가 예정) |
| 그룹 조회 | GET | /api/groups/{groupId} | 완료 | 그룹명·초대코드. "가족 코드 복사"·"초대 링크 복사" 공용 — 링크는 프론트가 코드로 딥링크 조립(별도 API 없음) |

### member — 온보딩(1-1~1-5) / 메인 격자 / 프로필

| 기능 | Method | Path | 상태 | 설명 |
|---|---|---|---|---|
| 그룹 참여 (가입) | POST | /api/members | 완료 | 이름+초대코드. 즉시 memberId 응답 → 얼굴 스캔 화면으로. 응답에 낭독 지정 문장(voiceScript) 포함. 코드 불일치 시 404 |
| 아바타 생성 (사진 업로드) | POST | /api/members/{memberId}/avatar | 완료 | 얼굴 사진 multipart — 촬영이든 앨범 선택이든 동일 계약(실시간 얼굴 스캔 아님). 즉시 응답 후 백그라운드 생성, "3D 이미지 변환 중" 화면은 아래 폴링으로 전환. 재호출 = 프로필 "사진 다시 찍기" |
| 보이스팩 등록 (낭독) | POST | /api/members/{memberId}/voicepack | 완료 | 온보딩 1-4-1 음성 인식 화면. 지정 문장 낭독 녹음 multipart, 즉시 응답 후 백그라운드 등록. 문장은 서버 템플릿("안녕하세요, 저는 {이름}입니다") — 가입 응답으로 내려준 것과 동일해야 함. **READY 되면 이어서 인사말 샘플 TTS 자동 생성** |
| 멤버 단건 (폴링·생성 완료 화면) | GET | /api/members/{memberId} | 완료 | avatarStatus·voiceStatus 폴링 + 1-5 생성 완료 화면 데이터: avatarUrl(3D 캐릭터) + greetingAudioUrl(내 목소리 인사말 샘플, 생성 전 null) — 이 둘로 "본인 음성으로 이야기하는 캐릭터" 연출 |
| 멤버 목록 (메인 격자) | GET | /api/members?groupId=&viewerId= | 완료 | 그룹 멤버 아바타+이름 + **말풍선 데이터**: viewerId 기준 멤버별 안읽은 메시지 수·최신 안읽은 메시지 미리보기(축약 텍스트·시각). 없으면 말풍선 필드 null. avatarUrl null이면 기본 캐릭터 |
| 프로필 수정 | PATCH | /api/members/{memberId} | 완료 | 이름 변경. 사진 변경은 avatar API 재호출 |

### message — 가족 채팅 (음성 ↔ 텍스트 변환이 핵심)

| 기능 | Method | Path | 상태 | 설명 |
|---|---|---|---|---|
| 텍스트 메시지 전송 | POST | /api/messages | 완료 | senderId+text. 즉시 응답, 백그라운드 TTS(보낸이 보이스팩)로 audioUrl 생성 → 시니어는 목소리로 듣기 |
| 음성 메시지 전송 | POST | /api/messages/voice | 완료 | senderId+녹음 multipart. 즉시 응답, 백그라운드 STT로 text 생성. 오디오는 원본 유지(재합성 안 함 — 02 §8-7) |
| 이미지 메시지 전송 | POST | /api/messages/image | 완료 | senderId+사진 multipart. AI 변환 없음 — 동기 저장, 즉시 완결 |
| 메시지 목록 (폴링 겸) | GET | /api/messages?groupId=&after={id} | 완료 | after 이후 메시지만 반환. 프론트가 1~2초 폴링 → 채팅 실시간성 확보 (WebSocket은 하루 범위에 과함) |
| 멤버 확대 뷰 (안읽은 원본) | GET | /api/members/{memberId}/messages?viewerId=&unreadOnly=true | 완료 | 아바타 클릭 시: 그 멤버가 보낸 안읽은 메시지 원본(오래된 순) — 프론트가 자동 재생. unreadOnly=false면 전체 이력 |
| 읽음 처리 | POST | /api/messages/read | 완료 | 두 시나리오를 하나로 처리: 확대 뷰 = {readerId, senderId} → 그 멤버 것만 / 채팅 진입 = {readerId} → 전체. GET에 읽음 부작용 안 태움(폴링과 분리). 멱등 — 격자 말풍선 제거 트리거 |

- TEXT·VOICE 메시지는 최종적으로 **텍스트+오디오 쌍**이 된다. 부족한 반쪽의 생성 상태가 `convertStatus`. READY 전엔 해당 필드 null → 프론트 폴백(텍스트는 그대로 표시 / 오디오는 표준 TTS). IMAGE는 변환 없이 즉시 완결.
- 채팅 이미지를 보관함(갤러리)에 자동 수집할지, 보관함 별도 업로드로 갈지는 **팀 확정 필요** — 자동 수집이면 아래 photo 업로드 API가 없어지고 채팅 IMAGE가 곧 갤러리 소스가 된다.

### photo — 추억 기록 갤러리

| 기능 | Method | Path | 상태 | 설명 |
|---|---|---|---|---|
| 사진 업로드 | POST | /api/photos | 완료 | uploaderId+사진 multipart(+위치 텍스트, 촬영일 선택 — 미입력 시 KST 업로드 날짜로 저장). AI 생성 없음 — 동기 처리 |
| 갤러리 목록 | GET | /api/photos?groupId=&date=YYYY-MM-DD | 완료 | KST 날짜 필터(캘린더 선택). date 없으면 전체 최신순. 카드에 작성자·위치 표시 |

### file — 정적 파일 서빙

| 기능 | Method | Path | 상태 | 설명 |
|---|---|---|---|---|
| 파일 서빙 | GET | /files/** | 완료 | 아바타 PNG·메시지 WAV·갤러리 사진. DB엔 경로만 저장, 실파일은 파일시스템 |

---

## familog-ai (`:8000`, server 전용 내부 API)

필드는 snake_case (Python 관례). 실패 시 500 → server가 해당 생성물 FAILED 전이.

| 기능 | Method | Path | 상태 | 설명 |
|---|---|---|---|---|
| 헬스체크 | GET | /health | 완료 | 서버 뼈대 생존 확인. AI 서버 구현 1순위 |
| 아바타 생성 | POST | /avatar | 예정 | 셀카+member_id → OpenAI 이미지 API(3D memoji 프롬프트 고정) → PNG 경로 |
| 보이스팩 등록 | POST | /voicepack | 완료 | 참조 오디오(multipart)+대사+member_id → ~/familog-data/voicepacks/{member_id}/ 저장 → `{voicepack_id: "vp_{member_id}"}` |
| TTS 변환 | POST | /tts | 완료 | JSON `{text, voicepack_id, output_name?}` → CosyVoice2 zero-shot → `{audio_path: "messages/{name}.wav"}` (데이터 디렉토리 기준 상대경로). `TTS_ENGINE=mock`이면 macOS say 대체 |
| STT 변환 | POST | /stt | 예정 | 음성 → 텍스트. 채팅 핵심 경로라 우선순위 상향. 엔진 후보: Whisper 로컬(무료) |
