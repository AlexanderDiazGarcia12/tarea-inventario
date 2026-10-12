package com.store.inventory.adapter;

import java.time.Instant;
import java.util.List;
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
    public Reservation confirm(String orderId, Instant now) {
        TrackedReservation tracked = byOrderId.get(orderId);
        if (tracked == null) {
            throw new IllegalStateException("No reservation for order " + orderId);
        }
        boolean confirmed = !tracked.hasExpiredAt(now) && tracked.confirm();
        if (!confirmed) {
            throw notConfirmable(tracked);
        }
        return tracked.reservation();
    }

    @Override
    public List<Reservation> expireDueAt(Instant now) {
        return byOrderId.values().stream()
                .filter(tracked -> tracked.expireIfDueAt(now))
                .map(TrackedReservation::reservation)
                .toList();
    }

    private static IllegalStateException notConfirmable(TrackedReservation tracked) {
        return switch (tracked.status()) {
            case CONFIRMED -> new IllegalStateException(
                    "Reservation for order " + tracked.reservation().orderId() + " is already confirmed");
            case EXPIRED, ACTIVE -> expired(tracked.reservation());
        };
    }

    private static IllegalStateException expired(Reservation reservation) {
        return new IllegalStateException(
                "Reservation for order " + reservation.orderId() + " expired at " + reservation.expiresAt());
    }
}
