package com.csbet.backend.casecatalog;

import java.time.LocalDateTime;
import java.util.List;
import com.csbet.backend.skin.Rarity;

public final class CatalogDtos {
    private CatalogDtos() { }
    public record CaseSummary(String id, String externalId, String name, String category, String type,
                              String description, String imageUrl, String language) {
        static CaseSummary from(WeaponCase box) {
            return new CaseSummary(box.getExternalId(), box.getExternalId(), box.getName(), box.getCategory(),
                    box.getType(), box.getDescription(), box.getImageUrl(), box.getLanguage());
        }
    }
    public record RarityResponse(String id, String name, String color) {
        static RarityResponse from(Rarity rarity) { return new RarityResponse(rarity.getId(), rarity.getName(), rarity.getColor()); }
    }
    public record ItemResponse(String id, String name, String weapon, String weaponType, String specialCategory,
                               String imageUrl, RarityResponse rarity, boolean special) {
        static ItemResponse from(CaseItem link) {
            var item = link.getItem();
            return new ItemResponse(item.getExternalId(), item.getName(), item.getWeapon(), item.getWeaponType(),
                    item.getSpecialCategory(), item.getImageUrl(), RarityResponse.from(item.getRarity()), link.isSpecial());
        }
    }
    public record CaseDetail(CaseSummary box, List<ItemResponse> items, List<ItemResponse> specialItems,
                             String specialNotice, String source, String sourceRevision, LocalDateTime importedAt,
                             String imageNotice) { }
    public record CasePage(List<CaseSummary> content, int page, int size, long totalElements, int totalPages) { }
}
