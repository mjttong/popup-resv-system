package com.mjttong.resv.reservation;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("reservation")
@Validated
public record ReservationProperties(@Positive int queueLimitMultiplier) {
}
