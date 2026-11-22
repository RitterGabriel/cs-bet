package com.csbet.backend.market;

import com.csbet.backend.casecatalog.WeaponCase;
import com.csbet.backend.casecatalog.WeaponCaseRepository;
import com.csbet.backend.skin.*;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class MarketRefreshTransactions {
    private final MarketControlRepository control;
    private final MarketVariantRepository variants;
    private final MarketHistoryRepository history;
    private final CosmeticItemRepository skins;
    private final WeaponCaseRepository cases;
    public MarketRefreshTransactions(MarketControlRepository control, MarketVariantRepository variants,
            MarketHistoryRepository history, CosmeticItemRepository skins, WeaponCaseRepository cases) {
        this.control = control; this.variants = variants; this.history = history; this.skins = skins; this.cases = cases;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(Instant now, Duration interval) {
        var state = Objects.requireNonNull(control.lockForUpdate(), "Controle de cotações ausente.");
        LocalDateTime time = utc(now);
        if (state.getNextAttemptAt() != null && state.getNextAttemptAt().isAfter(time)) return false;
        state.attempt(time, utc(now.plus(interval)));
        return true; // COMMIT antes da rede: falhas posteriores não apagam o controle da quota.
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(String status, Instant now) { control.lockForUpdate().finish(status, utc(now)); }

    public record Applied(int stored, int rejected) { }
    @Transactional
    public Applied apply(List<MarketPriceSource.Quote> batch, String provider, Instant now) {
        control.lockForUpdate();
        var skinIds = skins.findAllBySource("BYMYKEL").stream().collect(Collectors.toMap(CosmeticItem::getExternalId, CosmeticItem::getId));
        var caseIds = cases.findAllBySource("BYMYKEL").stream().collect(Collectors.toMap(WeaponCase::getExternalId, WeaponCase::getId));
        Map<String, MarketVariant> byHash = variants.findAll().stream().collect(Collectors.toMap(MarketVariant::getMarketHashName, Function.identity()));
        Map<String, MarketVariant> byTarget = byHash.values().stream().collect(Collectors.toMap(MarketRefreshTransactions::key, Function.identity()));
        Set<String> seen = new HashSet<>();
        int stored = 0, rejected = 0;
        for (var quote : batch) {
            if (!valid(quote, now)) { rejected++; continue; }
            Long skinId = quote.target() == MarketPriceSource.Target.SKIN ? skinIds.get(quote.externalId()) : null;
            Long caseId = quote.target() == MarketPriceSource.Target.CASE ? caseIds.get(quote.externalId()) : null;
            if (skinId == null && caseId == null) { rejected++; continue; }
            var candidate = new MarketVariant(skinId, caseId, quote);
            if (!seen.add(key(candidate))) { rejected++; continue; }
            var existing = byTarget.get(key(candidate));
            if (existing == null) existing = byHash.get(quote.marketHashName());
            if (existing != null && !existing.matches(skinId, caseId, quote)) { rejected++; continue; }
            if (existing != null && !utc(quote.quotedAt()).isAfter(existing.getQuotedAt())) continue;
            var variant = existing == null ? candidate : existing;
            variant.record(quote, provider, utc(now), utc(quote.quotedAt().plus(Duration.ofHours(24))));
            variant = variants.save(variant);
            history.save(new MarketHistory(variant));
            byHash.put(variant.getMarketHashName(), variant); byTarget.put(key(variant), variant);
            stored++;
        }
        return new Applied(stored, rejected);
    }
    static boolean valid(MarketPriceSource.Quote quote, Instant now) {
        if (quote == null || quote.target() == null || quote.wear() == null || quote.externalId() == null
                || quote.marketHashName() == null || quote.marketHashName().isBlank() || quote.marketHashName().length() > 300
                || !"BRL".equals(quote.currency()) || quote.amount() == null || quote.amount().signum() <= 0
                || quote.quotedAt() == null || quote.quotedAt().isAfter(now)
                || !quote.quotedAt().plus(Duration.ofHours(24)).isAfter(now) || (quote.statTrak() && quote.souvenir())) return false;
        if (quote.target() == MarketPriceSource.Target.CASE && (quote.wear() != Wear.NONE || quote.statTrak() || quote.souvenir())) return false;
        if (!Set.of("STEAM_COMMUNITY_MARKET", "MARKET_CSGO").contains(Objects.toString(quote.source(), ""))) return false;
        if ("MARKET_CSGO".equals(quote.source())) {
            if (!"USD".equals(quote.originalCurrency()) || quote.originalAmount() == null || quote.originalAmount().signum() <= 0
                    || quote.exchangeRate() == null || quote.exchangeRate().signum() <= 0 || quote.exchangeRateAt() == null
                    || quote.exchangeRateAt().isAfter(now) || quote.exchangeRateAt().plus(Duration.ofDays(10)).isBefore(now)) return false;
            try {
                if (quote.originalAmount().setScale(6, RoundingMode.UNNECESSARY).precision() > 18
                        || quote.exchangeRate().setScale(8, RoundingMode.UNNECESSARY).precision() > 18
                        || quote.originalAmount().multiply(quote.exchangeRate()).setScale(2, RoundingMode.HALF_UP).compareTo(quote.amount()) != 0) return false;
            } catch (ArithmeticException invalid) { return false; }
        }
        try { return quote.amount().setScale(2, RoundingMode.UNNECESSARY).precision() <= 18; }
        catch (ArithmeticException invalid) { return false; }
    }
    private static String key(MarketVariant variant) {
        return variant.getSkinId() + ":" + variant.getCaseId() + ":" + variant.getWear() + ":" + variant.isStatTrak() + ":" + variant.isSouvenir();
    }
    static LocalDateTime utc(Instant instant) { return LocalDateTime.ofInstant(instant, ZoneOffset.UTC).truncatedTo(java.time.temporal.ChronoUnit.MICROS); }
}
