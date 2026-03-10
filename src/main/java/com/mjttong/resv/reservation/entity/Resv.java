package com.mjttong.resv.reservation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resvs", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_RESV_USER_EVENT", columnNames = {"user_id", "event_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Resv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_id")
    private Queue queue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResvState state;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum ResvState {
        CONFIRMED, CANCELLED
    }
}
