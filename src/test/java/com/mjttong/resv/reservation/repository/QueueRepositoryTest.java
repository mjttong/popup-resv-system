package com.mjttong.resv.reservation.repository;

import com.mjttong.resv.TestcontainersConfiguration;
import com.mjttong.resv.reservation.entity.Event;
import com.mjttong.resv.reservation.entity.Queue;
import com.mjttong.resv.reservation.entity.Store;
import com.mjttong.resv.reservation.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class QueueRepositoryTest {

    private final TestEntityManager em;
    private final QueueRepository queueRepository;

    @Autowired
    QueueRepositoryTest(TestEntityManager em, QueueRepository queueRepository) {
        this.em = em;
        this.queueRepository = queueRepository;
    }

    @Test
    void countByEventIdReturns0WhenNoQueue() {
        Store store = em.persist(Store.builder().build());
        Event event = em.persist(eventOf(store));

        assertThat(queueRepository.countByEventId(event.getId())).isEqualTo(0L);
    }

    @Test
    void countByEventIdReturnsNWhenNItemsSaved() {
        Store store = em.persist(Store.builder().build());
        Event event = em.persist(eventOf(store));
        User user1 = em.persist(User.builder().build());
        User user2 = em.persist(User.builder().build());
        User user3 = em.persist(User.builder().build());
        em.persist(Queue.builder().event(event).user(user1).build());
        em.persist(Queue.builder().event(event).user(user2).build());
        em.persist(Queue.builder().event(event).user(user3).build());

        assertThat(queueRepository.countByEventId(event.getId())).isEqualTo(3L);
    }

    @Test
    void countByEventIdExcludesDifferentEvent() {
        Store store = em.persist(Store.builder().build());
        Event eventA = em.persist(eventOf(store));
        Event eventB = em.persist(eventOf(store));
        User user1 = em.persist(User.builder().build());
        User user2 = em.persist(User.builder().build());
        User user3 = em.persist(User.builder().build());
        User user4 = em.persist(User.builder().build());
        User user5 = em.persist(User.builder().build());
        em.persist(Queue.builder().event(eventA).user(user1).build());
        em.persist(Queue.builder().event(eventA).user(user2).build());
        em.persist(Queue.builder().event(eventB).user(user3).build());
        em.persist(Queue.builder().event(eventB).user(user4).build());
        em.persist(Queue.builder().event(eventB).user(user5).build());

        assertThat(queueRepository.countByEventId(eventA.getId())).isEqualTo(2L);
        assertThat(queueRepository.countByEventId(eventB.getId())).isEqualTo(3L);
    }

    private Event eventOf(Store store) {
        LocalDateTime now = LocalDateTime.now();
        return Event.builder()
                .store(store)
                .capacity(10)
                .remainingSlots(10)
                .openAt(now.minusHours(1))
                .closeAt(now.plusHours(1))
                .build();
    }
}
