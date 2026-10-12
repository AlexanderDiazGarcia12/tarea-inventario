package com.store.inventory.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.store.inventory.api.Reservation;

public interface ReservationRepository {

    Optional<Reservation> activeReservationFor(String orderId);

    Reservation saveIfAbsent(Reservation candidate);

    Reservation confirm(String orderId, Instant now);

    List<Reservation> expireDueAt(Instant now);
}
