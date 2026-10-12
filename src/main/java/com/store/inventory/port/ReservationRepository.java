package com.store.inventory.port;

import java.time.Instant;
import java.util.List;

import com.store.inventory.api.Reservation;

public interface ReservationRepository {

    void save(Reservation reservation);

    Reservation confirm(String orderId, Instant now);

    List<Reservation> expireDueAt(Instant now);
}
