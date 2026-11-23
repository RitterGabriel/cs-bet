package com.csbet.backend.casecatalog;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.csbet.backend.market.MarketDtos;
import java.math.BigDecimal;
import java.util.List;
public final class CustomCaseDtos {
    private CustomCaseDtos() { }
    public record Summary(String id,String name,String description,String imageUrl,String accent,
            @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal openingPrice,boolean available,int itemCount,String ruleVersion) { }
    public record Item(String id,String name,String weapon,String weaponType,String specialCategory,String imageUrl,
            CatalogDtos.RarityResponse rarity,boolean special,
            @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal customChance,List<MarketDtos.QuoteView> marketPrices) { }
    public record Detail(Summary box,List<Item> items,String notice) { }
    public record Page(List<Summary> content,int page,int size,long totalElements,int totalPages) { }
}
