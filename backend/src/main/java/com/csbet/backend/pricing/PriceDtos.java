package com.csbet.backend.pricing;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class PriceDtos {
    private PriceDtos() { }
    public record CurrentPrice(@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal basePrice,
                               @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal currentPrice,
                               LocalDate priceDate, OffsetDateTime updatedAt, String ruleVersion, String unit) {
        public static CurrentPrice from(SimulatedPrice price) {
            return new CurrentPrice(price.getBasePrice().setScale(2), price.getCurrentPrice().setScale(2), price.getPriceDate(),
                    price.getUpdatedAt().atOffset(ZoneOffset.UTC), price.getRuleVersion(), "SIMULATED_CREDITS");
        }
    }
    public record ItemPriceRange(@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal minimum,
                                 @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal maximum, long unpricedItems) { }
    public record HistoryPoint(LocalDate date, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount, String eventType) {
        static HistoryPoint from(PriceHistory history) { return new HistoryPoint(history.getPriceDate(), history.getAmount().setScale(2), history.getEventType()); }
    }
    public record HistoryPage(List<HistoryPoint> content, int page, int size, long totalElements, int totalPages, String unit) { }
}
