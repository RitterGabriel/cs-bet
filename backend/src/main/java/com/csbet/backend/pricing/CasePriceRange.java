package com.csbet.backend.pricing;
import java.math.BigDecimal;

public record CasePriceRange(Long caseId, BigDecimal minimum, BigDecimal maximum, Long itemCount, Long pricedCount) {
    public PriceDtos.ItemPriceRange response() {
        long missing = itemCount - pricedCount;
        return new PriceDtos.ItemPriceRange(missing == 0 ? minimum.setScale(2) : null, missing == 0 ? maximum.setScale(2) : null, missing);
    }
}
