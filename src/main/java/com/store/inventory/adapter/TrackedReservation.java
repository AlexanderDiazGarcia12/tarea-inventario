package com.store.inventory.adapter;

import com.store.inventory.api.Reservation;

final class TrackedReservation {

    private final Reservation reservation;
    private volatile ReservationStatus status;

    TrackedReservation(Reservation reservation) {
        this.reservation = reservation;
        this.status = ReservationStatus.ACTIVE;
    }

    Reservation reservation() {
        return reservation;
    }

    ReservationStatus status() {
        return status;
    }

    void markConfirmed() {
        this.status = ReservationStatus.CONFIRMED;
    }

}
