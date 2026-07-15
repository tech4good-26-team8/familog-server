# 컨벤션

협업·코드 규칙. 해커톤이라 **가볍게, 그러나 어기면 서로 헷갈리는 것만** 정한다.

---

## 1. Git

### 커밋 메시지

`타입: 한국어 설명` 형식. 본문은 필요할 때만.

```
feat: 멤버 가입 API 추가
fix: TTS 상태 전이 누락 수정
docs: API 표에 /stt 추가
```

| 타입 | 용도 |
|---|---|
| init | 프로젝트 초기 세팅 |
| feat | 기능 추가 |
| fix | 버그 수정 |
| refactor | 동작 변화 없는 구조 개선 |
| docs | 문서만 변경 |
| chore | 빌드·설정·의존성 등 잡일 |

### 브랜치

- 해커톤 속도 우선: **main 직접 push 허용.**
- 같은 파일을 동시에 만질 것 같으면 `feat/이름-작업` 브랜치 파서 짧게 쓰고 바로 머지.
- push 전에 `git pull --rebase` 습관화 (충돌 조기 발견).

### 저장소

- `familog-server`(Java/Spring), `familog-ai`(Python/FastAPI) 완전 분리. 문서는 server의 `docs/`가 단일 출처.
- 커밋 금지: API 키·`.env`, `venv/`, `build/`, 생성 결과물(`data/`), IDE 설정(`.idea/`).

---

## 2. 문서 갱신 규칙

- 엔드포인트 추가/변경 → `03_API_SPEC.md` 표를 **같은 커밋에서** 수정 (상태: 예정/WIP/완료).
- 기술 선택·변경의 "왜" → `02_TECH_FLOW.md` §8 결정 기록에 한 줄 추가.

---

## 3. API 공통 규칙

- server(프론트용) 경로: `/api/{복수형 리소스}` — 예: `/api/members`, `/api/posts/{postId}/replies`. 버저닝(`/v1`) 없음 — 하루짜리 데모에 버전이 생길 일 없다.
- server JSON 필드: **camelCase**. ai(내부) 필드: **snake_case**.
- 시간: ISO-8601 (`2026-07-16T10:30:00`). **타임존은 전 구간 Asia/Seoul(KST) 고정** — MySQL 연결(`serverTimezone=Asia/Seoul`)·Jackson(`spring.jackson.time-zone`)·API 응답 모두. UTC 저장/변환 안 함 (한국 단일 타임존 로컬 데모 — 변환 누락으로 날짜 밀리는 버그 원천 차단).
- 상태값 enum은 넷으로 고정: `PENDING / PROCESSING / READY / FAILED`.
- 파일 URL 필드는 READY 전엔 `null` — 프론트는 null이면 폴백(기본 캐릭터/표준 TTS) 표시.
- **공통 응답 래퍼(`CommonResponse<T>`) 미사용.** 성공은 DTO 그대로 + HTTP 상태코드, 실패만 아래 형식으로 통일. 프론트 파싱이 단순해지고, 래퍼 인프라 만들 시간을 아낀다.

```json
{ "code": "MEMBER_NOT_FOUND", "message": "존재하지 않는 멤버입니다." }
```

---

## 4. Java / Spring (familog-server)

### 네이밍

| 대상 | 규칙 | 예시 |
|---|---|---|
| 클래스명 | PascalCase | `Member`, `PostController`, `GenerationStatus` |
| 메서드/변수 | camelCase | `createMember`, `findById`, `memberId` |
| 상수 | SCREAMING_SNAKE_CASE | `DEFAULT_PAGE_SIZE` |
| enum 값 | SCREAMING_SNAKE_CASE | `PENDING`, `READY`, `FAILED` |
| DB 테이블명 | snake_case, **단수형** | `member`, `post`, `reply` |
| DB 컬럼명 | snake_case | `author_id`, `created_at`, `avatar_url` |
| PK 컬럼명 | `id` | JPA 기본 전략 그대로 (별도 매핑 설정 없이) |
| boolean 필드 | `is` 접두사 | `isLiked` |

테이블 단수형·PK `id`는 JPA 기본 네이밍을 그대로 타서 `@Table`/`@Column` 매핑 코드를 줄이기 위함.

### 클래스 어노테이션 순서

```java
// 엔티티
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity { ... }

// 컨트롤러
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController { ... }

// 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService { ... }   // 쓰기 메서드에만 @Transactional 오버라이드
```

### Lombok

- `@Getter`: 엔티티·DTO 전부. `@Setter`: **사용하지 않음** (상태 변경은 의도가 드러나는 메서드로 — `markReady(url)`, `markFailed()`).
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`: 모든 엔티티.
- `@Builder`: 엔티티 생성자 레벨에 선언 (클래스 레벨 금지).
- `@RequiredArgsConstructor` + `private final`: 생성자 주입 (필드 주입 금지).
- `@Slf4j`: 로깅 필요한 곳 (비동기 생성 서비스, 예외 핸들러).

### import

- 와일드카드(`*`) 미사용, 명시적 import만.

### 엔티티

- `BaseTimeEntity`(createdAt, updatedAt) 상속. **soft delete 없음** — 삭제 기능 자체가 범위 밖.
- PK 전략: `GenerationType.IDENTITY`.
- 연관관계: `@ManyToOne(fetch = FetchType.LAZY)` 고정. Cascade 미사용.
- 상태 필드는 공용 enum `GenerationStatus` 하나를 `@Enumerated(EnumType.STRING)`으로.

### DTO (record)

- 모든 DTO는 `record`. `dto` 패키지 아래 `request` / `response` 분리.
- 접미사: `~Request` / `~Response`.
- 정적 팩토리: 단일 변환 `from(entity)`, 복합/컬렉션 조립 `of(...)`.
- 엔티티를 컨트롤러 응답으로 직접 노출하지 않는다.

### Controller / Service

- Controller는 검증·DTO 변환만. 로직은 Service에.
- **Facade·Command VO 계층 없음** — Controller → Service 직행, Service 파라미터는 원시값 또는 Request 그대로. 도메인이 3개뿐이라 계층을 늘리면 잃는 게 더 많다.
- 비동기 생성 로직은 Service에서 `@Async` 메서드로 분리 (상태 전이 책임도 거기에).

### Validation

- Request DTO에 Jakarta Validation (`@NotBlank`, `@NotNull` 등), 메시지는 한글.
- Controller 파라미터에 `@Valid`. 실패 시 `GlobalExceptionHandler`가 400 + 첫 번째 FieldError 메시지 반환.

```java
public record CreatePostRequest(
        @NotNull(message = "작성자는 필수입니다.") Long authorId,
        @NotBlank(message = "내용을 입력해 주세요.") String content
) {}
```

### 예외 처리

- 에러 코드는 **enum 하나로 통합** (`ErrorCode` — 도메인별 분리는 이 규모에선 과함).
- 예외 클래스도 하나: `BusinessException(ErrorCode)`.
- `GlobalExceptionHandler`(@RestControllerAdvice)에서 `{code, message}` 형식으로 전역 처리.

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 멤버입니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시물입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
```

### 설정

- `@ConfigurationProperties` + record (`@Value` 미사용):

```java
@ConfigurationProperties(prefix = "familog.ai")
public record AiProperties(String baseUrl, Duration avatarTimeout, Duration ttsTimeout) {}
```

### 포맷팅

- 들여쓰기 4스페이스, 중괄호 같은 줄, 메서드 사이 빈 줄 1개, 줄 길이 120자 이내.

---

## 5. Python / FastAPI (familog-ai)

- snake_case (함수·변수·필드), 클래스 PascalCase.
- 엔드포인트는 `routers/`로 분리, 모델 추론 로직은 `services/`에 (라우터에 로직 넣지 않기).
- 요청/응답은 pydantic 모델로 명시.
- API 키 등 비밀값은 `.env` → 환경변수로만 읽는다. 코드·커밋에 키 금지.
- 생성 파일은 `data/` 아래에만 쓴다 (gitignore 대상).
