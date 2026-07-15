# ERD (MySQL)

테이블 4개. 상태값 enum은 전부 `PENDING / PROCESSING / READY / FAILED` (문자열 저장).

## 관계

- `family_group` 1 ── N `member` : 그룹 소속 (그룹 = 단톡방 1개, 별도 채팅방 테이블 없음)
- `family_group` 1 ── N `message` : 그룹 채팅
- `family_group` 1 ── N `photo` : 그룹 갤러리
- `member` 1 ── N `message` : 보낸 메시지
- `member` 1 ── N `photo` : 올린 사진
- `message` N ── N `member` : 읽음 여부 (`message_read` 매핑 테이블)

## family_group — 가족 그룹

| 컬럼 | 한국어명 | 타입 | 제약 | 설명 |
|---|---|---|---|---|
| id | 그룹 ID | BIGINT | PK, AUTO_INCREMENT | |
| name | 그룹명 | VARCHAR(50) | NOT NULL | 예: 김가네 |
| invite_code | 초대코드 | VARCHAR(6) | NOT NULL, UNIQUE | 6자리 |
| created_at | 생성일시 | DATETIME | NOT NULL | |

## member — 멤버 (가족·시니어)

| 컬럼 | 한국어명 | 타입 | 제약 | 설명 |
|---|---|---|---|---|
| id | 멤버 ID | BIGINT | PK, AUTO_INCREMENT | |
| group_id | 소속 그룹 ID | BIGINT | FK → family_group, NOT NULL | |
| name | 이름 | VARCHAR(30) | NOT NULL | |
| avatar_url | 아바타 경로 | VARCHAR(255) | NULL | 3D 캐릭터 PNG. READY 전 null |
| avatar_status | 아바타 생성 상태 | VARCHAR(15) | NOT NULL | 기본 PENDING |
| voicepack_id | 보이스팩 식별자 | VARCHAR(50) | NULL | 온보딩 낭독으로 등록. 등록 전 null |
| voice_status | 보이스팩 생성 상태 | VARCHAR(15) | NOT NULL | 기본 PENDING |
| greeting_audio_url | 인사말 샘플 경로 | VARCHAR(255) | NULL | 보이스팩 READY 후 자동 TTS("안녕! 나 {이름}이야" 등 고정 문장). 생성 전 null. 1-5 생성 완료 화면 재생용 |
| created_at | 가입일시 | DATETIME | NOT NULL | |
| updated_at | 수정일시 | DATETIME | NOT NULL | 프로필 수정 반영 |

## message — 채팅 메시지

| 컬럼 | 한국어명 | 타입 | 제약 | 설명 |
|---|---|---|---|---|
| id | 메시지 ID | BIGINT | PK, AUTO_INCREMENT | |
| group_id | 그룹 ID | BIGINT | FK → family_group, NOT NULL | |
| sender_id | 보낸 멤버 ID | BIGINT | FK → member, NOT NULL | |
| type | 메시지 유형 | VARCHAR(10) | NOT NULL | TEXT / VOICE / IMAGE |
| text | 텍스트 내용 | VARCHAR(500) | NULL | VOICE는 STT 완료 전 null. IMAGE는 항상 null |
| audio_url | 오디오 경로 | VARCHAR(255) | NULL | TEXT는 TTS 완료 전 null. IMAGE는 항상 null |
| image_url | 이미지 경로 | VARCHAR(255) | NULL | IMAGE 메시지만. 그 외 null |
| convert_status | 변환 상태 | VARCHAR(15) | NOT NULL | 부족한 반쪽(TTS/STT) 생성 상태. IMAGE는 변환 없음 → READY 고정 |
| created_at | 전송일시 | DATETIME | NOT NULL | 목록 정렬·폴링(after) 기준 |

TEXT·VOICE 메시지는 최종적으로 **텍스트 + 오디오 쌍**이 되고, IMAGE는 변환 없이 즉시 완결. `type`은 폴백·말풍선 UI 구분용.

## message_read — 메시지 읽음 기록

| 컬럼 | 한국어명 | 타입 | 제약 | 설명 |
|---|---|---|---|---|
| id | 읽음 ID | BIGINT | PK, AUTO_INCREMENT | |
| message_id | 메시지 ID | BIGINT | FK → message, NOT NULL | |
| reader_id | 읽은 멤버 ID | BIGINT | FK → member, NOT NULL | |
| read_at | 읽은 일시 | DATETIME | NOT NULL | |

- `UNIQUE(message_id, reader_id)` — 같은 메시지 중복 읽음 방지 (읽음 처리는 멱등).
- "안읽음" = 남이 보낸 메시지 중 내 read 행이 없는 것. **본인이 보낸 메시지는 대상 아님.**
- 메인 격자 말풍선: 멤버별 안읽은 메시지 존재 여부 + 최신 1건 미리보기의 근거 테이블.

## photo — 추억 기록 갤러리

| 컬럼 | 한국어명 | 타입 | 제약 | 설명 |
|---|---|---|---|---|
| id | 사진 ID | BIGINT | PK, AUTO_INCREMENT | |
| group_id | 그룹 ID | BIGINT | FK → family_group, NOT NULL | |
| uploader_id | 올린 멤버 ID | BIGINT | FK → member, NOT NULL | |
| image_url | 사진 경로 | VARCHAR(255) | NOT NULL | 업로드 즉시 저장 (AI 생성 없음) |
| location | 위치 | VARCHAR(100) | NULL | 위치 텍스트 (예: 강남역) |
| taken_date | 촬영일 | DATE | NOT NULL | 갤러리 날짜 필터 기준. KST 날짜 (미입력 시 업로드 시각의 KST 날짜) |
| created_at | 업로드일시 | DATETIME | NOT NULL | |

## 공통 규칙

- DB에는 **경로/식별자만** 저장. 바이너리(PNG·WAV·JPG)는 절대 DB에 넣지 않는다 (05 §6 파일 레이아웃 참고).
- FK는 JPA `@ManyToOne(fetch = LAZY)` 매핑, Cascade 미사용 (04 컨벤션).
- 메인 화면의 알림·Zzz(상태 표시)는 범위 미정 — 확정되면 테이블 추가.
