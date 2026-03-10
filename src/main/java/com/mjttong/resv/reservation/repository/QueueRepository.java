package com.mjttong.resv.reservation.repository;

import com.mjttong.resv.reservation.entity.Queue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueRepository extends JpaRepository<Queue, Long> {

    long countByEventId(Long eventId);
}
