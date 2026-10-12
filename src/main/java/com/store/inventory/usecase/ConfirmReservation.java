package com.store.inventory.usecase;

import java.time.Clock;
import java.time.Instant;

import com.store.inventory.api.Reservation;
import com.store.inventory.port.ReservationRepository;
import com.store.inventory.port.StockRepository;

public class ConfirmReservation {
    private final Clock clock;
    private final StockRepository stockRepository;
    private final ReservationRepository reservationRepository;
    private final ReleaseExpiredReservations releaseExpiredReservations;

    public ConfirmReservation(Clock clock, StockRepository stockRepository, ReservationRepository reservationRepository, ReleaseExpiredReservations releaseExpiredReservations) {
        this.clock = clock;
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
        this.releaseExpiredReservations = releaseExpiredReservations;
    }

    public void confirm(String orderId) {
        requireNonBlank(orderId, "orderId");
        Instant now = clock.instant();
        releaseExpiredReservations.releaseDueAt(now);

        Reservation reservation = reservationRepository.confirm(orderId, now);
        stockRepository.confirmUnits(reservation.sku(), reservation.quantity());
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
