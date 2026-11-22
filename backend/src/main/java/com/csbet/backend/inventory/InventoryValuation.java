package com.csbet.backend.inventory;
import java.math.BigDecimal;

public record InventoryValuation(BigDecimal pricedTotal, Long availableItems, Long unpricedItems) {
    public BigDecimal estimatedTotal() {
        if (unpricedItems != null && unpricedItems > 0) return null;
        return pricedTotal == null ? new BigDecimal("0.00") : pricedTotal.setScale(2);
    }
}
