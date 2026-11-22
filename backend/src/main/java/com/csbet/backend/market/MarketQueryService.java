package com.csbet.backend.market;

import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.csbet.backend.market.MarketDtos.*;

@Service
@Transactional(readOnly = true)
public class MarketQueryService {
    private final MarketVariantRepository variants;
    private final MarketHistoryRepository history;
    private final com.csbet.backend.skin.CosmeticItemRepository skins;
    private final com.csbet.backend.casecatalog.WeaponCaseRepository cases;
    private final Clock clock;
    public MarketQueryService(MarketVariantRepository variants, MarketHistoryRepository history,
            com.csbet.backend.skin.CosmeticItemRepository skins, com.csbet.backend.casecatalog.WeaponCaseRepository cases, Clock pricingClock) {
        this.variants = variants; this.history = history; this.skins = skins; this.cases = cases; this.clock = pricingClock;
    }
    public Map<Long, List<QuoteView>> skinQuotes(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return variants.findBySkinIdIn(ids).stream().collect(Collectors.groupingBy(MarketVariant::getSkinId,
                Collectors.mapping(variant -> QuoteView.from(variant, clock.instant()), Collectors.toList())));
    }
    public Map<Long, QuoteView> caseQuotes(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return variants.findByCaseIdIn(ids).stream().collect(Collectors.toMap(MarketVariant::getCaseId, variant -> QuoteView.from(variant, clock.instant())));
    }
    public Map<Long, QuoteView> variantQuotes(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return variants.findAllById(ids).stream().collect(Collectors.toMap(MarketVariant::getId, variant -> QuoteView.from(variant, clock.instant())));
    }
    public List<QuoteView> forSkin(String externalId) {
        var skin = skins.findBySourceAndExternalId("BYMYKEL", externalId).orElseThrow(MarketNotFoundException::new);
        return skinQuotes(List.of(skin.getId())).getOrDefault(skin.getId(), List.of());
    }
    public QuoteView forCase(String externalId) {
        var box = cases.findBySourceAndExternalId("BYMYKEL", externalId).orElseThrow(MarketNotFoundException::new);
        return caseQuotes(List.of(box.getId())).getOrDefault(box.getId(), QuoteView.unavailable());
    }
    public HistoryPage history(Long variantId, int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) throw new IllegalArgumentException("Paginação inválida.");
        if (!variants.existsById(variantId)) throw new MarketNotFoundException();
        var result = history.findByVariantId(variantId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "quotedAt", "id")));
        return new HistoryPage(result.getContent().stream().map(HistoryPoint::from).toList(), page, size, result.getTotalElements(), result.getTotalPages(), "MARKET_REFERENCE");
    }
}
