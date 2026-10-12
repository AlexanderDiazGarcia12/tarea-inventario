package com.store.inventory.domain.reservation;

import java.time.Instant;

import com.store.inventory.api.Reservation;

public class ReservationExpiry {

    private ReservationExpiry() {
    }

    public static boolean hasExpired(Reservation reservation, Instant now) {
        return !now.isBefore(reservation.expiresAt());
    }
}
