package com.store.inventory.adapter;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import com.store.inventory.api.Reservation;
import com.store.inventory.domain.reservation.ReservationExpiry;

final class TrackedReservation {

    private final Reservation reservation;
    private final AtomicReference<ReservationStatus> status = new AtomicReference<>(ReservationStatus.ACTIVE);

    TrackedReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    Reservation reservation() {
        return reservation;
    }

    ReservationStatus status() {
        return status.get();
    }

    boolean hasExpiredAt(Instant now) {
        return ReservationExpiry.hasExpired(reservation, now);
    }

    boolean confirm() {
        return status.compareAndSet(ReservationStatus.ACTIVE, ReservationStatus.CONFIRMED);
    }

    boolean expireIfDueAt(Instant now) {
        return hasExpiredAt(now) && status.compareAndSet(ReservationStatus.ACTIVE, ReservationStatus.EXPIRED);
    }
}
