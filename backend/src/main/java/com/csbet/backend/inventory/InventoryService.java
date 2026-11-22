package com.csbet.backend.inventory;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.csbet.backend.inventory.InventoryResponse.InventoryPage;

@Service
public class InventoryService {
    private final InventoryRepository repository;

    private final com.csbet.backend.pricing.PriceQueryService prices;
    private final com.csbet.backend.market.MarketQueryService market;
    private final java.time.Clock clock;
    public InventoryService(InventoryRepository repository, com.csbet.backend.pricing.PriceQueryService prices,
            com.csbet.backend.market.MarketQueryService market, java.time.Clock pricingClock) {
        this.repository = repository; this.prices = prices; this.market = market; this.clock = pricingClock;
    }

    // Mantém página e total no mesmo snapshot Oracle, mesmo durante a atualização diária.
    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    public InventoryPage findOwned(Long ownerId, int page, int size, InventoryStatus status) {
        if (page < 0 || page > 100000 || size < 1 || size > 60) throw new InvalidInventoryRequestException();
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "obtainedAt", "id"));
        var result = status == null ? repository.findByOwnerId(ownerId, pageable)
                : repository.findByOwnerIdAndStatus(ownerId, status, pageable);
        var current = prices.currentForIds(result.getContent().stream().map(item -> item.getSkin().getId()).distinct().toList());
        var quotes = market.variantQuotes(result.getContent().stream().map(InventoryItem::getMarketVariantId).filter(java.util.Objects::nonNull).distinct().toList());
        var valuation = repository.estimateAvailable(ownerId, InventoryStatus.AVAILABLE,
                java.time.LocalDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC));
        return new InventoryPage(result.getContent().stream().map(item -> InventoryResponse.from(item, current.get(item.getSkin().getId()), item.getMarketVariantId() == null ? com.csbet.backend.market.MarketDtos.QuoteView.unavailable() : quotes.getOrDefault(item.getMarketVariantId(), com.csbet.backend.market.MarketDtos.QuoteView.unavailable()))).toList(),
                page, size, result.getTotalElements(), result.getTotalPages(), valuation.estimatedTotal(),
                valuation.availableItems(), valuation.unpricedItems() == null ? 0 : valuation.unpricedItems(), "BRL");
    }
}
