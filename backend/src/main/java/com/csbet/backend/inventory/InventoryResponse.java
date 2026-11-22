package com.csbet.backend.inventory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record InventoryResponse(Long id, Long skinId, String externalId, String name, String imageUrl,
                                RarityResponse rarity, OffsetDateTime obtainedAt, InventoryStatus status) {
    public record RarityResponse(String id, String name, String color) { }
    public record InventoryPage(List<InventoryResponse> content, int page, int size, long totalElements, int totalPages) { }

    public static InventoryResponse from(InventoryItem item) {
        var skin = item.getSkin();
        var rarity = skin.getRarity();
        return new InventoryResponse(item.getId(), skin.getId(), skin.getExternalId(), skin.getName(),
                skin.getImageUrl(), new RarityResponse(rarity.getId(), rarity.getName(), rarity.getColor()),
                item.getObtainedAt().atOffset(ZoneOffset.UTC), item.getStatus());
    }
}
