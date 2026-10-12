package com.store.inventory.usecase;

import java.time.Clock;
import java.time.Instant;

import com.store.inventory.api.Reservation;
import com.store.inventory.port.ReservationRepository;
import com.store.inventory.port.StockRepository;

public final class ReleaseExpiredReservations {

    private final Clock clock;
    private final StockRepository stockRepository;
    private final ReservationRepository reservationRepository;
    
    public ReleaseExpiredReservations(Clock clock, StockRepository stockRepository, ReservationRepository reservationRepository) {
        this.clock = clock;
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
    }

    public void releaseDue() {
        releaseDueAt(clock.instant());
    }

    public void releaseDueAt(Instant now) {
        for (Reservation expired : reservationRepository.expireDueAt(now)) {
            stockRepository.releaseUnits(expired.sku(), expired.quantity());
        }
    }
}
