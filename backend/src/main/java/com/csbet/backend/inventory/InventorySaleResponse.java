package com.csbet.backend.inventory;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
public record InventorySaleResponse(Long saleId, Long inventoryId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal balance) {
    static InventorySaleResponse from(InventorySale sale) {
        return new InventorySaleResponse(sale.getId(), sale.getInventoryId(), sale.getAmount().setScale(2), sale.getResultingBalance().setScale(2));
    }
}
