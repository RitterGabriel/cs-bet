package com.csbet.backend.inventory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record InventoryResponse(Long id, Long skinId, String externalId, String name, String imageUrl,
                                RarityResponse rarity, OffsetDateTime obtainedAt, InventoryStatus status,
                                com.csbet.backend.pricing.PriceDtos.CurrentPrice simulatedPrice,
                                com.csbet.backend.market.MarketDtos.QuoteView marketPrice,
                                OffsetDateTime soldAt,
                                @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
                                java.math.BigDecimal salePrice) {
    public record RarityResponse(String id, String name, String color) { }
    public record InventoryPage(List<InventoryResponse> content, int page, int size, long totalElements, int totalPages,
            @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
            java.math.BigDecimal estimatedAvailableTotal, long availableItems, long unpricedAvailableItems, String valuationCurrency) { }

    public static InventoryResponse from(InventoryItem item) { return from(item, null, com.csbet.backend.market.MarketDtos.QuoteView.unavailable()); }
    public static InventoryResponse from(InventoryItem item, com.csbet.backend.pricing.PriceDtos.CurrentPrice price, com.csbet.backend.market.MarketDtos.QuoteView market) {
        var skin = item.getSkin();
        var rarity = skin.getRarity();
        return new InventoryResponse(item.getId(), skin.getId(), skin.getExternalId(), item.getDisplayName(),
                skin.getImageUrl(), new RarityResponse(rarity.getId(), rarity.getName(), rarity.getColor()),
                item.getObtainedAt().atOffset(ZoneOffset.UTC), item.getStatus(), price, market, item.getSoldAt() == null ? null : item.getSoldAt().atOffset(ZoneOffset.UTC),
                item.getSalePrice() == null ? null : item.getSalePrice().setScale(2));
    }
}
