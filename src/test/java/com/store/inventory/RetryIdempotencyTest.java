package com.store.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.store.inventory.api.InventoryService;
import com.store.inventory.api.ProductCategory;
import com.store.inventory.api.Reservation;

public class RetryIdempotencyTest {
    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
    private final InventoryService service = Inventory.create(clock, (sku, available) -> { });

    @Test 
    void reenvioIdenticoDevuelveLaReservaExistenteSinDescontarStock() {
        givenProduct("SKU-100", ProductCategory.STANDARD, 10);
        Reservation first = service.reserve("ORDER-1", "SKU-100", 3);
        int availableAfterFirstAttempt = service.available("SKU-100");

        clock.advance(Duration.ofMinutes(1));
        Reservation retried = service.reserve("ORDER-1", "SKU-100", 3);

        assertEquals(first, retried);
        assertEquals(7, availableAfterFirstAttempt);
        assertEquals(availableAfterFirstAttempt, service.available("SKU-100"));
    }

    @Test
    void reenvioIdenticoSobreFlashSaleNoCuentaContraElLimitePorPedido() {
        givenProduct("SKU-100", ProductCategory.FLASH_SALE, 10);
        Reservation first = service.reserve("ORDER-1", "SKU-100", 2);

        Reservation retried = service.reserve("ORDER-1", "SKU-100", 2);

        assertEquals(first, retried);
        assertEquals(8, service.available("SKU-100"));
    }

    @Test
    void reenvioConSkuDistintoLanzaIllegalStateSinModificarNada() {
        givenProduct("SKU-100", ProductCategory.STANDARD, 10);
        givenProduct("SKU-200", ProductCategory.STANDARD, 10);
        service.reserve("ORDER-1", "SKU-100", 3);

        IllegalStateException error =
                assertThrows(IllegalStateException.class, () -> service.reserve("ORDER-1", "SKU-200", 3));

        assertTrue(error.getMessage().contains("ORDER-1"), error.getMessage());
        assertEquals(7, service.available("SKU-100"));
        assertEquals(10, service.available("SKU-200"));
    }

    @Test
    void reenvioConCantidadDistintaLanzaIllegalStateSinModificarNada() {
        givenProduct("SKU-100", ProductCategory.STANDARD, 10);
        service.reserve("ORDER-1", "SKU-100", 3);

        assertThrows(IllegalStateException.class, () -> service.reserve("ORDER-1", "SKU-100", 4));

        assertEquals(7, service.available("SKU-100"));
    }

    @Test
    void conflictoSeReportaComoIllegalStateAunqueExcedaElLimitePorPedido() {
        givenProduct("SKU-100", ProductCategory.FLASH_SALE, 10);
        service.reserve("ORDER-1", "SKU-100", 2);

        assertThrows(IllegalStateException.class, () -> service.reserve("ORDER-1", "SKU-100", 5));

        assertEquals(8, service.available("SKU-100"));
    }

    @Test
    void reservaOriginalSigueConfirmableDespuesDeUnConflicto() {
        givenProduct("SKU-100", ProductCategory.STANDARD, 10);
        Reservation original = service.reserve("ORDER-1", "SKU-100", 3);
        assertThrows(IllegalStateException.class, () -> service.reserve("ORDER-1", "SKU-100", 4));

        service.confirm("ORDER-1");

        assertEquals(3, original.quantity());
        assertEquals(7, service.available("SKU-100"));
    }

    @Test
    void orderIdReutilizadoTrasExpirarNoSeTrataComoReintentoAunqueCoincidanLosDatos() {
        givenProduct("SKU-100", ProductCategory.STANDARD, 10);
        Reservation expired = service.reserve("ORDER-1", "SKU-100", 3);
        clock.advance(Duration.ofMinutes(15));

        Reservation renewed = service.reserve("ORDER-1", "SKU-100", 3);

        assertNotEquals(expired, renewed);
        assertEquals(clock.instant().plus(Duration.ofMinutes(15)), renewed.expiresAt());
        assertEquals(7, service.available("SKU-100"));
    }

    private void givenProduct(String sku, ProductCategory category, int units) {
        service.registerProduct(sku, category);
        service.addStock(sku, units);
    }
}
