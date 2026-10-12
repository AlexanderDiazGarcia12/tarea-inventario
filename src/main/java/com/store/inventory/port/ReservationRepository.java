package com.store.inventory.port;

import com.store.inventory.api.Reservation;

public interface ReservationRepository {

    void save(Reservation reservation);

    Reservation confirm(String orderId);
}
