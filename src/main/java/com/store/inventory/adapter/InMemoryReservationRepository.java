package com.store.inventory.adapter;

import java.util.concurrent.ConcurrentHashMap;

import com.store.inventory.api.Reservation;
import com.store.inventory.port.ReservationRepository;

public class InMemoryReservationRepository implements ReservationRepository {

    private final ConcurrentHashMap<String, TrackedReservation> byOrderId = new ConcurrentHashMap<>();

    @Override
    public void save(Reservation reservation) {
        byOrderId.put(reservation.orderId(), new TrackedReservation(reservation));
    }

    @Override
    public Reservation confirm(String orderId) {
        TrackedReservation tracked = byOrderId.get(orderId);
        if (tracked == null || tracked.status() != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("No active reservation found for order " + orderId);
        }

        tracked.markConfirmed();
        return tracked.reservation();
    }
}
