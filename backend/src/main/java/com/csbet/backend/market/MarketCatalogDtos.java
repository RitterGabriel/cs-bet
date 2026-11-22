package com.csbet.backend.market;

import com.csbet.backend.casecatalog.CatalogDtos.RarityResponse;
import java.util.List;

public final class MarketCatalogDtos {
    private MarketCatalogDtos() { }
    public record Skin(String id, String name, String weapon, String weaponType, String specialCategory,
                       String imageUrl, RarityResponse rarity, boolean special, List<MarketDtos.QuoteView> marketPrices) { }
    public record RarityFilter(String id, String name, String color) { }
    public record SkinPage(List<Skin> content, int page, int size, long totalElements, int totalPages,
                           List<String> weapons, List<RarityFilter> rarities) { }
}
