package com.csbet.backend.casecatalog;

import java.time.LocalDateTime;
import java.util.List;
import com.csbet.backend.skin.Rarity;

public final class CatalogDtos {
    private CatalogDtos() { }
    public record CaseSummary(String id, String externalId, String name, String category, String type,
                              String description, String imageUrl, String language,
                              com.csbet.backend.pricing.PriceDtos.ItemPriceRange simulatedItemPrices, com.csbet.backend.market.MarketDtos.QuoteView marketPrice) {
        static CaseSummary from(WeaponCase box, com.csbet.backend.pricing.PriceDtos.ItemPriceRange range, com.csbet.backend.market.MarketDtos.QuoteView market) {
            return new CaseSummary(box.getExternalId(), box.getExternalId(), box.getName(), box.getCategory(),
                    box.getType(), box.getDescription(), box.getImageUrl(), box.getLanguage(), range, market);
        }
    }
    public record RarityResponse(String id, String name, String color) {
        static RarityResponse from(Rarity rarity) { return new RarityResponse(rarity.getId(), rarity.getName(), rarity.getColor()); }
    }
    public record ItemResponse(String id, String name, String weapon, String weaponType, String specialCategory,
                               String imageUrl, RarityResponse rarity, boolean special, com.csbet.backend.pricing.PriceDtos.CurrentPrice simulatedPrice, java.util.List<com.csbet.backend.market.MarketDtos.QuoteView> marketPrices, OfficialCaseOdds.Chance dropChance, String phase) {
        static ItemResponse from(CaseItem link, com.csbet.backend.pricing.PriceDtos.CurrentPrice price, java.util.List<com.csbet.backend.market.MarketDtos.QuoteView> quotes, OfficialCaseOdds.Chance chance) {
            return from(link,price,quotes,chance,null);
        }
        static ItemResponse from(CaseItem link, com.csbet.backend.pricing.PriceDtos.CurrentPrice price, java.util.List<com.csbet.backend.market.MarketDtos.QuoteView> quotes, OfficialCaseOdds.Chance chance, String phase) {
            var item = link.getItem();
            return new ItemResponse(item.getExternalId(), item.getName(), item.getWeapon(), item.getWeaponType(),
                    item.getSpecialCategory(), item.getImageUrl(), RarityResponse.from(item.getRarity()), link.isSpecial(), price, quotes, chance, phase);
        }
    }
    public record SpecialRewardGroup(OfficialCaseOdds.Chance dropChance, String selectionRule, boolean configured) {
        public static final String UNIFORM_RULE = "CSBET_UNIFORM_CATALOG_ITEM";
    }
    public record CaseDetail(CaseSummary box, List<ItemResponse> items, List<ItemResponse> specialItems,
                             String specialNotice, String source, String sourceRevision, LocalDateTime importedAt,
                             String imageNotice, SpecialRewardGroup specialReward) { }
    public record CasePage(List<CaseSummary> content, int page, int size, long totalElements, int totalPages) { }
}
