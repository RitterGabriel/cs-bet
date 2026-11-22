package com.csbet.backend.pricing;

import com.csbet.backend.skin.CosmeticItemRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.csbet.backend.pricing.PriceDtos.*;

@Service
@Transactional(readOnly = true)
public class PriceQueryService {
    private final SimulatedPriceRepository prices;
    private final PriceHistoryRepository history;
    private final CosmeticItemRepository skins;
    public PriceQueryService(SimulatedPriceRepository prices, PriceHistoryRepository history, CosmeticItemRepository skins) {
        this.prices = prices; this.history = history; this.skins = skins;
    }
    public Map<Long, CurrentPrice> currentForIds(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return prices.findBySkinIdIn(ids).stream().collect(Collectors.toMap(SimulatedPrice::getSkinId, CurrentPrice::from));
    }
    private Long skinId(String externalId) {
        return skins.findBySourceAndExternalId("BYMYKEL", externalId).orElseThrow(PriceNotFoundException::new).getId();
    }
    public CurrentPrice current(String externalId) {
        return prices.findById(skinId(externalId)).map(CurrentPrice::from).orElseThrow(PriceNotFoundException::new);
    }
    public HistoryPage history(String externalId, int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 365) throw new IllegalArgumentException("Paginação inválida.");
        var result = history.findBySkinId(skinId(externalId), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "priceDate", "id")));
        return new HistoryPage(result.getContent().stream().map(HistoryPoint::from).toList(), page, size,
                result.getTotalElements(), result.getTotalPages(), "SIMULATED_CREDITS");
    }
}
