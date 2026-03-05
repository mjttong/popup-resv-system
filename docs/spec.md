## 주제

- 팝업스토어 예약 시스템
- 공정성 보장을 1순위로 하는 예약 시스템

## 도메인 용어 정의

- 팝업스토어란, 한정된 시간과 제한된 슬롯을 바탕으로 일시적으로 열리는 오프라인 매장을 의미
- 예약이란, 팝업스토어에 오프라인 방문 권한을 주는 슬롯을 일시적으로 할당받는 것을 의미

## 아키텍처 구조: 공정성 보장 시나리오

분기 기준은 고정 N이 아니라 `Event.remaining_slots`와 대기열 깊이를 기반으로 한다.
`queue_limit = Event.capacity × N` (N은 설정값)

모든 요청은 `open_at <= NOW() <= close_at` 조건을 먼저 확인한다.
(MVP: 요청 시점에 직접 체크 / Post-MVP: TaskScheduler로 전환 예정)

- 1단계: 즉시 처리
    - 조건: `remaining_slots > 0`
    - 슬롯이 남아 있는 경우 Atomic UPDATE로 즉시 점유
    - 큐, 상태, 멱등 등이 불필요
    - 점유 성공 → `Resv(confirmed)` 생성 / 실패 → 2단계 분기
- 2단계: 대기열
    - 조건: `remaining_slots == 0` AND `대기 수 ≤ queue_limit`
    - 슬롯 소진 후 취소로 인한 재배정 가능성이 있는 구간
    - `Queue.id` 오름차순(AUTO_INCREMENT)으로 순서 보장, 공정성 유지
    - `confirmed → cancelled` 발생 시 다음 Queue.id에게 슬롯 자동 재배정
- 3단계: 차단
    - 조건: `대기 수 > queue_limit`
    - DB 접근 없이 즉시 거절 응답 반환

## API 스펙

공통 응답 래퍼 없이 HTTP 상태코드로 성공/실패를 구분한다.
(MVP: 래퍼 불필요 / Post-MVP 클라이언트 추가 시 도입 검토)

| 메서드      | 엔드포인트                                            | 설명          |
|----------|--------------------------------------------------|-------------|
| `POST`   | `/events/{eventId}/reservations`                 | 예약 요청       |
| `DELETE` | `/events/{eventId}/reservations`                 | 예약/대기 취소    |
| `GET`    | `/events/{eventId}/reservations`                 | 예약/대기 상태 조회 |

**사용자 식별**: 모든 메서드는 `X-User-Id: {userId}` 헤더로 사용자를 전달한다.
(MVP 제약: 인증 없이 클라이언트가 직접 값을 지정. 위변조 가능하며, 인증 도입 시 헤더 추출 로직만 교체)

## DB 구조

```mysql
CREATE TABLE `Store`
(
    `id` int NOT NULL
);

CREATE TABLE `Event`
(
    `id`              int      NOT NULL,
    `store_id`        int      NOT NULL,
    `capacity`        int      NOT NULL,
    `remaining_slots` int      NOT NULL,
    `open_at`         datetime NOT NULL,
    `close_at`        datetime NOT NULL
);

CREATE TABLE `Queue`
(
    `id`       int NOT NULL AUTO_INCREMENT,
    `event_id` int NOT NULL,
    `user_id`  int NOT NULL
);

CREATE TABLE `User`
(
    `id` int NOT NULL
);

CREATE TABLE `Resv`
(
    `id`         int                            NOT NULL,
    `user_id`    int                            NOT NULL,
    `event_id`   int                            NOT NULL,
    `queue_id`   int                            NULL,
    `state`      enum ('confirmed','cancelled') NOT NULL,
    `created_at` datetime                       NOT NULL
);

ALTER TABLE `Store`
    ADD CONSTRAINT `PK_STORE` PRIMARY KEY (
                                           `id`
        );

ALTER TABLE `Event`
    ADD CONSTRAINT `PK_EVENT` PRIMARY KEY (
                                           `id`
        );

ALTER TABLE `Queue`
    ADD CONSTRAINT `PK_QUEUE` PRIMARY KEY (
                                           `id`
        );

ALTER TABLE `User`
    ADD CONSTRAINT `PK_USER` PRIMARY KEY (
                                          `id`
        );

ALTER TABLE `Resv`
    ADD CONSTRAINT `PK_RESV` PRIMARY KEY (
                                          `id`
        );

ALTER TABLE `Resv`
    ADD CONSTRAINT `UQ_RESV_USER_EVENT` UNIQUE (
                                                `user_id`,
                                                `event_id`
        );

ALTER TABLE `Event`
    ADD CONSTRAINT `FK_Store_TO_Event_1` FOREIGN KEY (
                                                      `store_id`
        )
        REFERENCES `Store` (
                            `id`
            );

ALTER TABLE `Queue`
    ADD CONSTRAINT `FK_Event_TO_Queue_1` FOREIGN KEY (
                                                      `event_id`
        )
        REFERENCES `Event` (
                            `id`
            );

ALTER TABLE `Queue`
    ADD CONSTRAINT `FK_User_TO_Queue_1` FOREIGN KEY (
                                                     `user_id`
        )
        REFERENCES `User` (
                           `id`
            );

ALTER TABLE `Resv`
    ADD CONSTRAINT `FK_User_TO_Resv_1` FOREIGN KEY (
                                                    `user_id`
        )
        REFERENCES `User` (
                           `id`
            );

ALTER TABLE `Resv`
    ADD CONSTRAINT `FK_Event_TO_Resv_1` FOREIGN KEY (
                                                     `event_id`
        )
        REFERENCES `Event` (
                            `id`
            );

ALTER TABLE `Resv`
    ADD CONSTRAINT `FK_Queue_TO_Resv_1` FOREIGN KEY (
                                                     `queue_id`
        )
        REFERENCES `Queue` (
                            `id`
            );
```