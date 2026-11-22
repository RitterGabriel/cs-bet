package com.csbet.backend.market;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CSBET_MARKET_VARIANTS", schema = "CSBET")
public class MarketVariant {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "marketVariantSequence")
    @SequenceGenerator(name = "marketVariantSequence", sequenceName = "CSBET.SEQ_CSBET_MARKET_VARIANTS", allocationSize = 1)
    private Long id;
    private Long skinId;
    private Long caseId;
    @Column(nullable = false, length = 300) private String marketHashName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 8) private Wear wear;
    @Column(nullable = false, length = 1) private String statTrak;
    @Column(nullable = false, length = 1) private String souvenir;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal priceAmount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, length = 40) private String quoteSource;
    @Column(nullable = false, length = 100) private String provider;
    @Column(nullable = false) private LocalDateTime quotedAt;
    @Column(nullable = false) private LocalDateTime fetchedAt;
    @Column(nullable = false) private LocalDateTime validUntil;
    @Column(precision = 18, scale = 6) private BigDecimal originalAmount;
    @Column(length = 3) private String originalCurrency;
    @Column(precision = 18, scale = 8) private BigDecimal exchangeRate;
    private LocalDateTime exchangeRateAt;
    protected MarketVariant() { }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public String getOriginalCurrency() { return originalCurrency; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public LocalDateTime getExchangeRateAt() { return exchangeRateAt; }
    public MarketVariant(Long skinId, Long caseId, MarketPriceSource.Quote quote) {
        this.skinId = skinId; this.caseId = caseId; this.marketHashName = quote.marketHashName();
        this.wear = quote.wear(); this.statTrak = quote.statTrak() ? "Y" : "N"; this.souvenir = quote.souvenir() ? "Y" : "N";
    }
    public boolean matches(Long skinId, Long caseId, MarketPriceSource.Quote quote) {
        return java.util.Objects.equals(this.skinId, skinId) && java.util.Objects.equals(this.caseId, caseId)
                && marketHashName.equals(quote.marketHashName()) && wear == quote.wear()
                && isStatTrak() == quote.statTrak() && isSouvenir() == quote.souvenir();
    }
    public void record(MarketPriceSource.Quote quote, String provider, LocalDateTime now, LocalDateTime expires) {
        priceAmount = quote.amount().setScale(2); currency = quote.currency(); quoteSource = quote.source();
        originalAmount = quote.originalAmount(); originalCurrency = quote.originalCurrency(); exchangeRate = quote.exchangeRate();
        exchangeRateAt = quote.exchangeRateAt() == null ? null : MarketRefreshTransactions.utc(quote.exchangeRateAt());
        this.provider = provider; quotedAt = MarketRefreshTransactions.utc(quote.quotedAt());
        fetchedAt = now; validUntil = expires;
    }
    public Long getId() { return id; }
    public Long getSkinId() { return skinId; }
    public Long getCaseId() { return caseId; }
    public String getMarketHashName() { return marketHashName; }
    public Wear getWear() { return wear; }
    public boolean isStatTrak() { return "Y".equals(statTrak); }
    public boolean isSouvenir() { return "Y".equals(souvenir); }
    public BigDecimal getPriceAmount() { return priceAmount; }
    public String getCurrency() { return currency; }
    public String getQuoteSource() { return quoteSource; }
    public String getProvider() { return provider; }
    public LocalDateTime getQuotedAt() { return quotedAt; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public LocalDateTime getValidUntil() { return validUntil; }
}
