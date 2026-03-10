package com.mjttong.resv.reservation.service;

import com.mjttong.resv.reservation.ReservationProperties;
import com.mjttong.resv.reservation.ReservationStage;
import com.mjttong.resv.reservation.entity.Event;
import com.mjttong.resv.reservation.entity.Store;
import com.mjttong.resv.reservation.repository.EventRepository;
import com.mjttong.resv.reservation.repository.QueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationRouterTest {

    @Mock
    EventRepository eventRepository;

    @Mock
    QueueRepository queueRepository;

    ReservationRouter router;

    static final int MULTIPLIER = 3;
    static final int CAPACITY = 10;
    static final long EVENT_ID = 1L;

    @BeforeEach
    void setUp() {
        router = new ReservationRouter(
                eventRepository, queueRepository, new ReservationProperties(MULTIPLIER)
        );
    }

    // ── 분기 경계 테스트 ────────────────────────────────────

    @Test
    void stage1_remainingSlotsN_returns_STAGE_1() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(openEvent(5)));

        assertThat(router.route(EVENT_ID)).isEqualTo(ReservationStage.STAGE_1);
    }

    @Test
    void stage1_end_remainingSlots1_returns_STAGE_1() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(openEvent(1)));

        assertThat(router.route(EVENT_ID)).isEqualTo(ReservationStage.STAGE_1);
    }

    @Test
    void stage2_start_queueDepth0_returns_STAGE_2() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(openEvent(0)));
        when(queueRepository.countByEventId(EVENT_ID)).thenReturn(0L);

        assertThat(router.route(EVENT_ID)).isEqualTo(ReservationStage.STAGE_2);
    }

    @Test
    void stage2_inner_queueDepthM_returns_STAGE_2() {
        long mid = (long) (CAPACITY * MULTIPLIER) / 2;
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(openEvent(0)));
        when(queueRepository.countByEventId(EVENT_ID)).thenReturn(mid);

        assertThat(router.route(EVENT_ID)).isEqualTo(ReservationStage.STAGE_2);
    }

    @Test
    void stage2_end_queueDepthLimit_returns_STAGE_2() {
        long limit = (long) CAPACITY * MULTIPLIER;
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(openEvent(0)));
        when(queueRepository.countByEventId(EVENT_ID)).thenReturn(limit);

        assertThat(router.route(EVENT_ID)).isEqualTo(ReservationStage.STAGE_2);
    }

    @Test
    void stage3_start_queueDepthLimitPlus1_returns_STAGE_3() {
        long limit = (long) CAPACITY * MULTIPLIER;
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(openEvent(0)));
        when(queueRepository.countByEventId(EVENT_ID)).thenReturn(limit + 1);

        assertThat(router.route(EVENT_ID)).isEqualTo(ReservationStage.STAGE_3);
    }

    // ── 이벤트 시간 유효성 테스트 ────────────────────────────

    @Test
    void validTime_proceeds_to_stage() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(openEvent(5)));

        assertThat(router.route(EVENT_ID)).isEqualTo(ReservationStage.STAGE_1);
    }

    @Test
    void notYetOpened_throws_409() {
        LocalDateTime now = LocalDateTime.now();
        Event event = eventOf(0, now.plusHours(1), now.plusHours(2));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> router.route(EVENT_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void alreadyClosed_throws_409() {
        LocalDateTime now = LocalDateTime.now();
        Event event = eventOf(0, now.minusHours(2), now.minusHours(1));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> router.route(EVENT_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void bothOutside_throws_409() {
        LocalDateTime now = LocalDateTime.now();
        Event event = eventOf(0, now.plusHours(1), now.minusHours(1));
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> router.route(EVENT_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    // ── 헬퍼 ────────────────────────────────────────────────

    private Event openEvent(int remainingSlots) {
        LocalDateTime now = LocalDateTime.now();
        return eventOf(remainingSlots, now.minusHours(1), now.plusHours(1));
    }

    private Event eventOf(int remainingSlots, LocalDateTime openAt, LocalDateTime closeAt) {
        return Event.builder()
                .id(EVENT_ID)
                .store(Store.builder().id(1L).build())
                .capacity(CAPACITY)
                .remainingSlots(remainingSlots)
                .openAt(openAt)
                .closeAt(closeAt)
                .build();
    }
}
