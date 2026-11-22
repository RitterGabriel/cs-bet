package com.csbet.backend.market;

import com.csbet.backend.casecatalog.CatalogDtos.RarityResponse;
import java.time.Clock;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.csbet.backend.market.MarketCatalogDtos.*;

@Service
@Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
public class MarketCatalogService {
    private final MarketCatalogRepository repository;
    private final MarketQueryService quotes;
    private final Clock clock;
    public MarketCatalogService(MarketCatalogRepository repository, MarketQueryService quotes, Clock pricingClock) {
        this.repository=repository; this.quotes=quotes; this.clock=pricingClock;
    }
    public SkinPage list(int page, int size, String search, String weapon, String rarity, String wear, String sort) {
        if (page<0 || page>100000 || size<1 || size>60 || search.length()>150 || weapon.length()>120 || rarity.length()>80
                || !Set.of("name","name_desc","price_asc","price_desc").contains(sort)) throw new IllegalArgumentException();
        Wear selectedWear = wear.isEmpty() ? null : Wear.valueOf(wear);
        var result = repository.skins(search,weapon,rarity,selectedWear,sort,PageRequest.of(page,size),MarketRefreshTransactions.utc(clock.instant()));
        var prices = quotes.skinQuotes(result.getContent().stream().map(com.csbet.backend.skin.CosmeticItem::getId).toList());
        var content = result.getContent().stream().map(item -> new Skin(item.getExternalId(),item.getName(),item.getWeapon(),item.getWeaponType(),
                item.getSpecialCategory(),item.getImageUrl(),new RarityResponse(item.getRarity().getId(),item.getRarity().getName(),item.getRarity().getColor()),
                !"none".equals(item.getSpecialCategory()), prices.getOrDefault(item.getId(),List.of()).stream()
                    .filter(quote -> selectedWear==null || quote.wear()==selectedWear).toList())).toList();
        return new SkinPage(content,page,size,result.getTotalElements(),result.getTotalPages(),repository.weapons(),repository.rarities());
    }
}
