package com.store.inventory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.store.inventory.api.InventoryService;
import com.store.inventory.api.ProductCategory;
import com.store.inventory.api.Reservation;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReservationExpirationTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
    private final InventoryService service = Inventory.create(clock, (sku, available) -> { });

    @Test
    void liberacionAutomaticaPorVencimientoStandard() {
        givenProduct("SKU-100", ProductCategory.STANDARD, 10);
        service.reserve("ORDER-1", "SKU-100", 3);

        clock.advance(Duration.ofMinutes(15));

        assertEquals(10, service.available("SKU-100"));
    }

    @Test
    void confirmarUnaReservaYaExpiradaFalla() {
        givenProduct("SKU-100", ProductCategory.STANDARD, 10);
        service.reserve("ORDER-1", "SKU-100", 3);

        clock.advance(Duration.ofMinutes(16));

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.confirm("ORDER-1"));
        assertTrue(error.getMessage().contains("expired"), error.getMessage());
        assertEquals(10, service.available("SKU-100"));
    }

    @Test
    void confirmarUnPedidoQueNuncaReservoFallaSinMencionarExpiracion() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.confirm("ORDER-404"));
        assertTrue(error.getMessage().contains("No reservation"), error.getMessage());
    }

    @Test
    void reservaVigenteJustoAntesDelVencimientoSePuedeConfirmar() {
        givenProduct("SKU-100", ProductCategory.FLASH_SALE, 10);
        service.reserve("ORDER-5", "SKU-100", 2);

        clock.advance(Duration.ofMinutes(4).plusSeconds(59));

        assertDoesNotThrow(() -> service.confirm("ORDER-5"));
        assertEquals(8, service.available("SKU-100"));
    }

    @Test
    void instanteExactoDelVencimientoSeConsideraExpirado() {
        givenProduct("SKU-100", ProductCategory.FLASH_SALE, 10);
        Reservation reservation = service.reserve("ORDER-6", "SKU-100", 2);

        clock.advance(Duration.ofMinutes(5));

        assertEquals(reservation.expiresAt(), clock.instant());
        assertThrows(IllegalStateException.class, () -> service.confirm("ORDER-6"));
        assertEquals(10, service.available("SKU-100"));
    }

    @Test
    void orderIdReutilizableDespuesDeQueSuReservaExpiro() {
        givenProduct("SKU-100", ProductCategory.STANDARD, 10);
        service.reserve("ORDER-1", "SKU-100", 3);
        clock.advance(Duration.ofMinutes(15));

        Reservation renewed = service.reserve("ORDER-1", "SKU-100", 5);

        assertEquals(5, renewed.quantity());
        assertEquals(5, service.available("SKU-100"));
    }

    @Test
    void reutilizarOrderIdParaOtroSkuNoDejaUnidadesAtrapadasEnElPrimero() {
        givenProduct("SKU-100", ProductCategory.STANDARD, 10);
        givenProduct("SKU-200", ProductCategory.STANDARD, 10);
        service.reserve("ORDER-1", "SKU-100", 3);
        clock.advance(Duration.ofMinutes(15));

        service.reserve("ORDER-1", "SKU-200", 4);

        assertEquals(10, service.available("SKU-100"));
        assertEquals(6, service.available("SKU-200"));
    }

    @Test
    void unidadesConfirmadasNoVuelvenAlDisponibleCuandoPasaElPlazo() {
        givenProduct("SKU-100", ProductCategory.FLASH_SALE, 10);
        service.reserve("ORDER-7", "SKU-100", 2);
        service.confirm("ORDER-7");

        clock.advance(Duration.ofMinutes(10));

        assertEquals(8, service.available("SKU-100"));
    }

    private void givenProduct(String sku, ProductCategory category, int units) {
        service.registerProduct(sku, category);
        service.addStock(sku, units);
    }
}
