package com.store.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.store.inventory.api.InsufficientStockException;
import com.store.inventory.api.InventoryService;
import com.store.inventory.api.OrderLimitExceededException;
import com.store.inventory.api.ProductCategory;
import com.store.inventory.api.Reservation;

public class ReserveOrderTest {

    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final InventoryService service = Inventory.create(clock, (sku, available) -> {
    });

    @Test
    void reservaExitosaConStockSuficiente() {
        service.registerProduct("SKU-100", ProductCategory.STANDARD);
        service.addStock("SKU-100", 10);

        Reservation reservation = service.reserve("ORDER-1", "SKU-100", 3);

        assertEquals(now.plus(Duration.ofMinutes(15)), reservation.expiresAt());
        assertEquals(7, service.available("SKU-100"));
    }

    @Test
    void stockInsuficienteNoAlteraElDisponible() {
        service.registerProduct("SKU-100", ProductCategory.STANDARD);
        service.addStock("SKU-100", 2);

        assertThrows(InsufficientStockException.class, () -> service.reserve("ORDER-2", "SKU-100", 5));
        assertEquals(2, service.available("SKU-100"));
    }

    @Test
    void productoDesconocidoFallaComoStockInsuficiente() {
        assertThrows(InsufficientStockException.class,
                () -> service.reserve("ORDER-3", "SKU-DESCONOCIDO", 1));
    }

    @Test
    void limitePorPedidoExcedidoEnFlashSaleNoAlteraElDisponible() {
        service.registerProduct("SKU-200", ProductCategory.FLASH_SALE);
        service.addStock("SKU-200", 50);

        assertThrows(OrderLimitExceededException.class, () -> service.reserve("ORDER-4", "SKU-200", 3));
        assertEquals(50, service.available("SKU-200"));
    }

    @Test
    void cantidadInvalidaFallaConIllegalArgumentException() {
        service.registerProduct("SKU-100", ProductCategory.STANDARD);

        assertThrows(IllegalArgumentException.class, () -> service.reserve("ORDER-5", "SKU-100", 0));
    }

    @Test
    void expiracionStandardEsQuinceMinutos() {
        service.registerProduct("SKU-STD", ProductCategory.STANDARD);
        service.addStock("SKU-STD", 1);

        Reservation reservation = service.reserve("ORDER-STD", "SKU-STD", 1);

        assertEquals(now.plus(Duration.ofMinutes(15)), reservation.expiresAt());
    }

    @Test
    void expiracionPreOrderEsVeinticuatroHoras() {
        service.registerProduct("SKU-PRE", ProductCategory.PRE_ORDER);
        service.addStock("SKU-PRE", 1);

        Reservation reservation = service.reserve("ORDER-PRE", "SKU-PRE", 1);

        assertEquals(now.plus(Duration.ofHours(24)), reservation.expiresAt());
    }

    @Test
    void expiracionFlashSaleEsCincoMinutos() {
        service.registerProduct("SKU-FLASH", ProductCategory.FLASH_SALE);
        service.addStock("SKU-FLASH", 1);

        Reservation reservation = service.reserve("ORDER-FLASH", "SKU-FLASH", 1);

        assertEquals(now.plus(Duration.ofMinutes(5)), reservation.expiresAt());
    }

    @Test
    void standardYPreOrderNoTienenLimitePorPedido() {
        service.registerProduct("SKU-STD", ProductCategory.STANDARD);
        service.addStock("SKU-STD", 500);
        service.registerProduct("SKU-PRE", ProductCategory.PRE_ORDER);
        service.addStock("SKU-PRE", 500);

        service.reserve("ORDER-STD", "SKU-STD", 500);
        service.reserve("ORDER-PRE", "SKU-PRE", 500);

        assertEquals(0, service.available("SKU-STD"));
        assertEquals(0, service.available("SKU-PRE"));
    }

    @Test
    void flashSaleAceptaHastaDosUnidadesPorPedido() {
        service.registerProduct("SKU-FLASH", ProductCategory.FLASH_SALE);
        service.addStock("SKU-FLASH", 10);

        service.reserve("ORDER-FLASH", "SKU-FLASH", 2);

        assertEquals(8, service.available("SKU-FLASH"));
    }
}
