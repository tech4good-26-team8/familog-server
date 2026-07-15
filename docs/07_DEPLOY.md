# 배포

GCP GPU VM **한 대**에 전부 올리는 배포 문서 — 무엇을, 어떤 순서로, 어떻게.
구조는 `05_ARCHITECTURE.md`의 localhost 구성을 VM으로 그대로 이식한다 (계약 변경 없음).

---

## 1. 배포 전략

```
                         GCP VM (GPU 1대, asia-northeast3 서울)
                        ┌─────────────────────────────────────┐
[프론트] ──REST:8080──▶ │ MySQL ← familog-server ──▶ familog-ai │
                        │              └── ~/familog-data ──┘   │ ← 공유 디렉토리 유지
                        └─────────────────────────────────────┘
```

- **왜 한 대에 다**: 두 서버가 `~/familog-data`를 파일시스템으로 공유하는 계약(05 §6)이라,
  같은 VM이면 코드 수정 없이 그대로 성립. 해커톤 기간 비용 차이도 미미.
- **왜 GPU**: CPU(M2) TTS는 RTF 14.4 — 문장당 2분+. GPU(T4/L4)는 RTF 1 이하로 문장당 몇 초.
- 외부에 여는 포트는 **8080 하나**. 8000(ai)과 3306(MySQL)은 VM 내부 통신만.

## 2. 사전 준비 (배포 전, 로컬에서)

| 항목 | 내용 | 상태 |
|---|---|---|
| GPU 쿼터 신청 | 콘솔 → IAM → 할당량 → `GPUs (all regions)` 1개 + 리전 GPU 쿼터. 승인 몇 분~몇 시간 | ☐ |
| afconvert → ffmpeg 교체 | `cosyvoice_tts.py`의 참조 오디오 변환이 **macOS 전용 afconvert** 사용. 리눅스용 ffmpeg 분기 필요 | ☐ |
| OPENAI_API_KEY 준비 | 아바타 생성용. VM의 `familog-ai/.env`에 넣는다 (커밋 금지, 04 §5) | ☐ |
| ai-mock 끄기 | `application.yml` `familog.ai-mock: false` (또는 환경변수로 오버라이드) | ☐ |

> MockEngine(`say`)도 macOS 전용이지만 VM에선 실제 엔진을 쓰므로 교체 불필요.

## 3. VM 생성

| 항목 | 값 | 비고 |
|---|---|---|
| 머신 | `g2-standard-4` (L4 24GB) | 대안: `n1-standard-4` + T4 스팟 (더 쌈, 회수 리스크) |
| 리전 | `asia-northeast3` (서울) | 프론트 지연 최소 |
| 이미지 | **Deep Learning VM (CUDA 12.x, PyTorch)** | CUDA 드라이버 설치 생략 가능 |
| 디스크 | 100GB SSD | 모델 ~5GB + venv + MySQL 여유 |
| 방화벽 | tcp:8080 허용 태그 | 8000/3306은 열지 않는다 |

```bash
gcloud compute instances create familog \
  --zone=asia-northeast3-a --machine-type=g2-standard-4 \
  --image-family=common-cu124 --image-project=deeplearning-platform-release \
  --boot-disk-size=100GB --tags=familog-server
gcloud compute firewall-rules create allow-familog \
  --allow=tcp:8080 --target-tags=familog-server
```

## 4. VM 초기 세팅 (1회)

```bash
# 1) 기본 도구
sudo apt update && sudo apt install -y openjdk-17-jdk mysql-server ffmpeg git
sudo timedatectl set-timezone Asia/Seoul

# 2) MySQL
sudo mysql -e "CREATE DATABASE familog; ALTER USER 'root'@'localhost' IDENTIFIED BY '<비밀번호>';"

# 3) 코드
git clone <familog-server 저장소> ~/familog-server
git clone <familog-ai 저장소> ~/familog-ai

# 4) familog-ai 환경
cd ~/familog-ai
python3.11 -m venv venv && venv/bin/pip install -r requirements.txt
echo "OPENAI_API_KEY=..." > .env
# CosyVoice2 모델 다운로드 (gitignore 대상이라 VM에서 직접)
venv/bin/python -c "from modelscope import snapshot_download; \
  snapshot_download('iic/CosyVoice2-0.5B', local_dir='pretrained_models/CosyVoice2-0.5B')"

# 5) familog-server 빌드
cd ~/familog-server && ./gradlew build
```

## 5. systemd 서비스 등록 (1회)

`/etc/systemd/system/familog-ai.service`:
```ini
[Unit]
Description=familog-ai
After=network.target
[Service]
User=<유저>
WorkingDirectory=/home/<유저>/familog-ai
ExecStart=/home/<유저>/familog-ai/venv/bin/uvicorn main:app --port 8000
Restart=on-failure
[Install]
WantedBy=multi-user.target
```

`/etc/systemd/system/familog-server.service`:
```ini
[Unit]
Description=familog-server
After=mysql.service familog-ai.service
[Service]
User=<유저>
Environment=DB_PASSWORD=<비밀번호>
ExecStart=/usr/bin/java -jar /home/<유저>/familog-server/build/libs/familog-server-0.0.1-SNAPSHOT.jar --familog.ai-mock=false
Restart=on-failure
[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now familog-ai familog-server
```

## 6. 재배포 (코드 수정 시)

개발은 계속 로컬(mock)에서, VM 반영은 필요할 때만. `deploy.sh` 한 번이면 끝:

```bash
#!/bin/bash
set -e
cd ~/familog-ai && git pull && sudo systemctl restart familog-ai        # 모델 재로드 1~2분
cd ~/familog-server && git pull && ./gradlew build -q && sudo systemctl restart familog-server
curl -sf localhost:8000/health && curl -sf localhost:8080/actuator/health || echo "확인 필요"
```

- familog-ai 재시작 = CosyVoice2 재로드(GPU 1~2분). **데모 직전엔 재배포 금지(프리징)**.

## 7. 검증 & 데모 당일 체크리스트

```bash
# VM에서 — 로컬 검증과 동일한 계약 확인
curl -X POST localhost:8000/voicepack -F audio=@test.m4a -F "script=..." -F member_id=1
curl -X POST localhost:8000/tts -H 'Content-Type: application/json' \
  -d '{"text":"테스트 문장","voicepack_id":"vp_1"}'   # 몇 초 내 응답이면 GPU 정상
```

- ☐ 스팟 썼다면 데모 당일 **온디맨드로 전환** (회수 방지)
- ☐ 첫 요청 프리워밍 (모델 로드·워밍업 완료 확인)
- ☐ 프론트 base URL을 VM 외부 IP:8080으로 변경
- ☐ 폴백: 데모용 결과 미리 생성해 심어두기 (02 §데모 전략)
