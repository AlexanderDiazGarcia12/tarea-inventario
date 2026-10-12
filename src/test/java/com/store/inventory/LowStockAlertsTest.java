package com.store.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.store.inventory.api.InventoryService;
import com.store.inventory.api.ProductCategory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LowStockAlertsTest {

    private record LowStockAlert(String sku, int availableUnits) {
    }

    private final List<LowStockAlert> alerts = new ArrayList<>();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
    private final InventoryService service =
            Inventory.create(clock, (sku, availableUnits) -> alerts.add(new LowStockAlert(sku, availableUnits)));

    @Test
    void primerDisparoDeAlertaAlCruzarElUmbral() {
        givenProduct("SKU-100", 6);

        service.reserve("ORDER-1", "SKU-100", 1);

        assertEquals(List.of(new LowStockAlert("SKU-100", 5)), alerts);
    }

    @Test
    void noRepetirLaAlertaMientrasSigueBajoElUmbral() {
        givenProduct("SKU-100", 6);
        service.reserve("ORDER-1", "SKU-100", 1);

        service.reserve("ORDER-2", "SKU-100", 1);

        assertEquals(4, service.available("SKU-100"));
        assertEquals(List.of(new LowStockAlert("SKU-100", 5)), alerts);
    }

    @Test
    void reactivarLaAlertaTrasReabastecerConAddStockYVolverACaer() {
        givenProduct("SKU-100", 6);
        service.reserve("ORDER-1", "SKU-100", 1);

        service.addStock("SKU-100", 5);
        service.reserve("ORDER-2", "SKU-100", 5);

        assertEquals(List.of(
                new LowStockAlert("SKU-100", 5),
                new LowStockAlert("SKU-100", 5)), alerts);
    }

    @Test
    void liberarUnaReservaExpiradaTambienReactivaLaAlerta() {
        givenProduct("SKU-100", 10);
        service.reserve("ORDER-1", "SKU-100", 5);

        clock.advance(Duration.ofMinutes(15));
        assertEquals(10, service.available("SKU-100"));
        service.reserve("ORDER-2", "SKU-100", 6);

        assertEquals(List.of(
                new LowStockAlert("SKU-100", 5),
                new LowStockAlert("SKU-100", 4)), alerts);
    }

    @Test
    void addStockQueNoSuperaElUmbralNoReactivaLaAlerta() {
        givenProduct("SKU-100", 6);
        service.reserve("ORDER-1", "SKU-100", 6);

        service.addStock("SKU-100", 2);

        assertEquals(2, service.available("SKU-100"));
        assertEquals(List.of(new LowStockAlert("SKU-100", 0)), alerts);
    }

    @Test
    void confirmarUnaReservaNoEmiteAvisos() {
        givenProduct("SKU-100", 6);
        service.reserve("ORDER-1", "SKU-100", 1);

        service.confirm("ORDER-1");

        assertEquals(5, service.available("SKU-100"));
        assertEquals(List.of(new LowStockAlert("SKU-100", 5)), alerts);
    }

    @Test
    void lasAlertasDeUnSkuNoAfectanAOtro() {
        givenProduct("SKU-100", 6);
        givenProduct("SKU-200", 6);
        service.reserve("ORDER-1", "SKU-100", 1);

        service.reserve("ORDER-2", "SKU-200", 1);

        assertEquals(List.of(
                new LowStockAlert("SKU-100", 5),
                new LowStockAlert("SKU-200", 5)), alerts);
    }

    private void givenProduct(String sku, int units) {
        service.registerProduct(sku, ProductCategory.STANDARD);
        service.addStock(sku, units);
    }
}
