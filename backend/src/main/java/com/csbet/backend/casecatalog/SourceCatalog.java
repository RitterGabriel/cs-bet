package com.csbet.backend.casecatalog;

import java.util.List;

public record SourceCatalog(List<SourceCase> cases, List<SourceItem> items, String revision, String language) {
    public record SourceRarity(String id, String name, String color) { }
    public record SourceItem(String id, String name, String weapon, String weaponType,
                             String specialCategory, String imageUrl, SourceRarity rarity) { }
    public record SourceLink(String itemId, boolean special) { }
    public record SourceCase(String id, String name, String description, String imageUrl,
                             List<SourceLink> links, String specialNotice) { }
}
