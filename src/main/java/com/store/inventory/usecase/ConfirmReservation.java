package com.store.inventory.usecase;

import com.store.inventory.api.Reservation;
import com.store.inventory.port.ReservationRepository;
import com.store.inventory.port.StockRepository;

public class ConfirmReservation {
    private final StockRepository stockRepository;
    private final ReservationRepository reservationRepository;

    public ConfirmReservation(StockRepository stockRepository, ReservationRepository reservationRepository) {
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
    }

    public void confirm(String orderId) {
        requireNonBlank(orderId, "orderId");
        Reservation reservation = reservationRepository.confirm(orderId);
        stockRepository.confirmUnits(reservation.sku(), reservation.quantity());
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
