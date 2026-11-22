package com.csbet.backend.market;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class MarketDtos {
    private MarketDtos() { }
    private static OffsetDateTime offset(LocalDateTime time) { return time == null ? null : time.atOffset(ZoneOffset.UTC); }
    public record QuoteView(Long variantId, String status,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal currentPrice, String currency,
            String source, String provider, OffsetDateTime quotedAt, OffsetDateTime validUntil,
            Wear wear, boolean statTrak, boolean souvenir, String marketHashName,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal originalAmount, String originalCurrency,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal exchangeRate, OffsetDateTime exchangeRateAt) {
        public static QuoteView unavailable() { return new QuoteView(null, "UNAVAILABLE", null, "BRL", null, null, null, null, null, false, false, null, null, null, null, null); }
        public static QuoteView from(MarketVariant variant, Instant now) {
            boolean fresh = variant.getValidUntil().isAfter(MarketRefreshTransactions.utc(now));
            return new QuoteView(variant.getId(), fresh ? "AVAILABLE" : "STALE",
                    fresh ? variant.getPriceAmount().setScale(2) : null, variant.getCurrency(), variant.getQuoteSource(), variant.getProvider(),
                    variant.getQuotedAt().atOffset(ZoneOffset.UTC), variant.getValidUntil().atOffset(ZoneOffset.UTC),
                    variant.getWear(), variant.isStatTrak(), variant.isSouvenir(), variant.getMarketHashName(),
                    variant.getOriginalAmount(), variant.getOriginalCurrency(), variant.getExchangeRate(), offset(variant.getExchangeRateAt()));
        }
    }
    public record HistoryPoint(@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount, String currency,
            String source, String provider, OffsetDateTime quotedAt,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal originalAmount, String originalCurrency,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal exchangeRate, OffsetDateTime exchangeRateAt) {
        static HistoryPoint from(MarketHistory point) {
            return new HistoryPoint(point.getPriceAmount().setScale(2), point.getCurrency(), point.getQuoteSource(), point.getProvider(), point.getQuotedAt().atOffset(ZoneOffset.UTC), point.getOriginalAmount(), point.getOriginalCurrency(), point.getExchangeRate(), offset(point.getExchangeRateAt()));
        }
    }
    public record HistoryPage(List<HistoryPoint> content, int page, int size, long totalElements, int totalPages, String origin) { }
}
