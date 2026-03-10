package com.mjttong.resv.reservation.service;

import com.mjttong.resv.reservation.ReservationProperties;
import com.mjttong.resv.reservation.ReservationStage;
import com.mjttong.resv.reservation.entity.Event;
import com.mjttong.resv.reservation.repository.EventRepository;
import com.mjttong.resv.reservation.repository.QueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationRouter {

    private final EventRepository eventRepository;
    private final QueueRepository queueRepository;
    private final ReservationProperties properties;

    public ReservationStage route(long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(event.getOpenAt()) || now.isAfter(event.getCloseAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event is not open");
        }

        if (event.getRemainingSlots() > 0) {
            return ReservationStage.STAGE_1;
        }

        long queueDepth = queueRepository.countByEventId(eventId);
        long queueLimit = (long) event.getCapacity() * properties.queueLimitMultiplier();

        if (queueDepth <= queueLimit) {
            return ReservationStage.STAGE_2;
        }

        return ReservationStage.STAGE_3;
    }
}
