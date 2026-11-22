package com.csbet.backend.casecatalog;

import java.util.Locale;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.csbet.backend.casecatalog.CatalogDtos.*;

@Service
@Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
public class CaseCatalogService {
    private final com.csbet.backend.market.MarketCatalogRepository marketCatalog;
    private final java.time.Clock clock;
    private final WeaponCaseRepository cases;
    private final CaseItemRepository contents;
    private final com.csbet.backend.pricing.PriceQueryService prices;
    private final com.csbet.backend.market.MarketQueryService market;
    public CaseCatalogService(WeaponCaseRepository cases, CaseItemRepository contents, com.csbet.backend.pricing.PriceQueryService prices,
            com.csbet.backend.market.MarketQueryService market, com.csbet.backend.market.MarketCatalogRepository marketCatalog, java.time.Clock pricingClock) {
        this.marketCatalog = marketCatalog; this.clock = pricingClock;
        this.cases = cases; this.contents = contents; this.prices = prices; this.market = market;
    }
    public CasePage list(int page, int size, String search, String category, String sort) {
        if (page < 0 || page > 100000 || size < 1 || size > 60 || search.length() > 150
                || !(category.equals("official") || category.equals("custom"))
                || !(java.util.Set.of("name", "name_desc", "featured", "price_asc", "price_desc").contains(sort))) throw new InvalidCatalogRequestException();
        String escaped = search.strip().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
        Specification<WeaponCase> filter = (root, query, cb) -> cb.and(
                cb.equal(root.get("category"), category),
                cb.like(cb.lower(root.get("name")), "%" + escaped + "%", '!'));
        // "featured" preserva a regra histórica: ID externo em ordem decrescente, sem ranking.
        Sort ordering = sort.equals("name") ? Sort.by("name", "externalId")
                : sort.equals("name_desc") ? Sort.by(Sort.Order.desc("name"), Sort.Order.asc("externalId")) : Sort.by(Sort.Direction.DESC, "externalId");
        var result = sort.startsWith("price_")
                ? marketCatalog.cases(search, category, sort, PageRequest.of(page,size), java.time.LocalDateTime.ofInstant(clock.instant(),java.time.ZoneOffset.UTC))
                : cases.findAll(filter, PageRequest.of(page, size, ordering));
        var ranges = priceRanges(result.getContent().stream().map(WeaponCase::getId).toList());
        var quotes = market.caseQuotes(result.getContent().stream().map(WeaponCase::getId).toList());
        return new CasePage(result.getContent().stream().map(box -> CaseSummary.from(box, ranges.get(box.getId()),
                quotes.getOrDefault(box.getId(), com.csbet.backend.market.MarketDtos.QuoteView.unavailable()))).toList(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }
    public CaseDetail detail(String externalId) {
        var box = cases.findBySourceAndExternalId(CatalogImportService.SOURCE, externalId)
                .orElseThrow(CatalogNotFoundException::new);
        var links = contents.findContents(box.getId());
        var current = prices.currentForIds(links.stream().map(link -> link.getItem().getId()).distinct().toList());
        var quotes = market.skinQuotes(links.stream().map(link -> link.getItem().getId()).distinct().toList());
        var boxQuote = market.caseQuotes(java.util.List.of(box.getId())).getOrDefault(box.getId(), com.csbet.backend.market.MarketDtos.QuoteView.unavailable());
        java.util.function.Function<CaseItem, ItemResponse> response = link -> ItemResponse.from(link,
                current.get(link.getItem().getId()), quotes.getOrDefault(link.getItem().getId(), java.util.List.of()));
        return new CaseDetail(CaseSummary.from(box, priceRanges(java.util.List.of(box.getId())).get(box.getId()), boxQuote),
                links.stream().filter(link -> !link.isSpecial()).map(response).toList(),
                links.stream().filter(CaseItem::isSpecial).map(response).toList(),
                box.getSpecialNotice(), "https://github.com/ByMykel/CSGO-API", box.getSourceRevision(), box.getImportedAt(),
                "URLs da fonte. Artes pertencem à Valve; a licença MIT dos dados não concede licença sobre essas imagens.");
    }
    private java.util.Map<Long, com.csbet.backend.pricing.PriceDtos.ItemPriceRange> priceRanges(java.util.List<Long> ids) {
        if (ids.isEmpty()) return java.util.Map.of();
        return contents.findPriceRanges(ids).stream().collect(java.util.stream.Collectors.toMap(
                com.csbet.backend.pricing.CasePriceRange::caseId, com.csbet.backend.pricing.CasePriceRange::response));
    }

}
