package com.mjttong.resv.reservation.repository;

import com.mjttong.resv.reservation.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
