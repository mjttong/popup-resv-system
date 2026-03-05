## 설계 결정 기록 (ADR)

---

### 1. Store / Event 분리

동일 팝업스토어의 다회성 이벤트를 가정하여 `Store`와 별개의 `Event` 엔티티 도입. 단일 Store에서 여러 Event가 독립적인 슬롯·스케줄을 가질 수 있음.

---

### 2. 분기 기준: capacity 기반

고정 N(1/1000/10000번째) 기준은 이벤트 규모와 무관한 임의값이다. `queue_limit = capacity × N`으로 대체하여 이벤트 규모에 비례한 대기열 한도를 설정.

---

### 3. Resv는 슬롯 확정 시에만 생성

`Resv = 예약 성공 기록`으로 도메인을 정의. 대기 진입 시 Resv를 생성하면 "예약 시도 기록"으로 의미가 바뀌어 도메인 정의와 충돌. 대기 중인 사용자는 `Queue` 레코드로만 추적.

**취소 시 `remaining_slots`를 경유하지 않는 이유**

취소 트랜잭션 안에서 다음 Queue 대기자에게 직접 `Resv(confirmed)`를 생성한다. `remaining_slots + 1`을 먼저 하면 잠깐이라도 `remaining_slots > 0`이 되는 순간 동시 1단계 요청이 슬롯을 가로챌 수 있기 때문이다.

---

### 4. Race Condition: Atomic UPDATE

```sql
UPDATE Event SET remaining_slots = remaining_slots - 1
WHERE id = ? AND remaining_slots > 0
```

낙관적 락은 Burst 시 충돌률 100%로 비효율. 비관적 락은 직렬화 병목 우려. Redis는 `remaining_slots` UPDATE가 병목임을 측정으로 확인한 후 검토.

---

### 5. Queue.seq 제거

`Queue.id` AUTO_INCREMENT가 seq 역할을 대체 (ORDER BY id ASC). 별도 seq 컬럼 불필요.

---

### 6. 3단계: AtomicLong 강제 근거

"DB 접근 없이 즉시 차단"이라는 조건이 AtomicLong을 강제한다. DB 조회·Redis 모두 네트워크 왕복이 발생하므로 이 조건을 위반한다.

Post-MVP: 수평 확장 시 Redis INCR/DECR로 전환 검토.

---

### 7. Resv.state: `failed` 미채택

`failed` 추가 시 도메인 정의(`Resv = 예약 성공 기록`)와 충돌. 1단계 실패·3단계 차단은 Queue를 거치지 않으므로 단계마다 Resv 유무도 달라진다. 실패 이력 추적이 필요해지면 컬럼·상태 추가 마이그레이션으로 확장 가능.

---

### 8. API: 공통 응답 래퍼 미채택

HTTP 상태코드만으로 충분. 클라이언트가 생길 때 도입 검토.

`X-User-Id` 헤더 방식: 인증 도입 시 컨트롤러/인터셉터의 헤더 추출 로직만 교체하면 되어 서비스 레이어 변경 불필요.
