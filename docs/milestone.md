## milestone

---

### 1. 오케스트레이터 구현

**목표**
Event 단위로 요청을 받아 `remaining_slots`와 대기열 깊이를 기준으로 1·2·3단계 중 하나로 분기한다.

**범위**
- DB 마이그레이션 (전체 테이블)
- `open_at <= NOW() <= close_at` 조건 체크 (이벤트 유효성 확인)
- 분기 기준: `remaining_slots > 0` → 1단계 / 대기 수 ≤ `queue_limit` → 2단계 / 초과 → 3단계
- `queue_limit = capacity × N` (N은 설정값)

**완료 기준**
- [ ] 각 경계 조건에서 올바른 단계로 분기됨 (테스트 통과)

---

### 2. 공정성 1단계 구현 (즉시 처리)

**목표**
`remaining_slots > 0`인 경우 슬롯을 race condition 없이 원자적으로 점유한다.

**범위**
- 슬롯 점유 성공 → `Resv(confirmed)` 생성
- 슬롯 점유 실패 → 2단계로 분기
- `confirmed → cancelled` 시: 대기자 없으면 `remaining_slots + 1`
  - 대기자 있는 경우의 재배정은 M3에서 검증

**완료 기준**
분기
- [ ] 동시 요청 시 슬롯을 race condition 없이 원자적으로 점유함 (테스트 통과)
- [ ] 슬롯 소진 후 요청은 2단계로 정확히 분기됨 (테스트 통과)

취소
- [ ] `confirmed → cancelled` 후 대기자 없으면 `remaining_slots + 1`됨 (테스트 통과)

---

### 3. 공정성 2단계 구현 (대기열)

**목표**
슬롯 소진 시 `Queue.id` 순서를 보장하여 공정하게 처리한다.

**범위**
- Queue 진입
- 슬롯 배정: `Queue.id` 오름차순으로 처리
- 대기 중 취소: Queue 레코드 삭제
- `confirmed → cancelled` 시: 다음 `Queue.id`에게 `Resv(confirmed)` 재배정; 대기자 없으면 `remaining_slots + 1`

**완료 기준**
- [ ] 슬롯 재배정 시 `Queue.id` 오름차순(가장 먼저 진입한 대기자)으로 배정됨 (동시 진입 테스트 통과)
- [ ] `confirmed → cancelled` 후 대기자 있으면 `Queue.id` 오름차순으로 재배정됨 (테스트 통과)

---

### 4. 공정성 3단계 구현 (차단)

**목표**
대기 수가 `queue_limit`를 초과한 요청을 DB 접근 없이 즉시 거절한다.

**범위**
- `queue_limit` 초과 시 즉시 거절 응답 반환
- DB 접근 없음 (읽기·쓰기 모두)
- 대기 수 추적: `AtomicLong` 인메모리 카운터 사용

**완료 기준**
- [ ] DB 접근 없이 거절됨 (응답 경로 테스트 통과)
- [ ] `queue_limit` 경계값에서 2단계/3단계 분기가 정확함 (테스트 통과)
