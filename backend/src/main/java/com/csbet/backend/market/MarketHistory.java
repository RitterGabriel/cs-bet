package com.csbet.backend.market;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CSBET_MARKET_HISTORY", schema = "CSBET")
public class MarketHistory {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "marketHistorySequence")
    @SequenceGenerator(name = "marketHistorySequence", sequenceName = "CSBET.SEQ_CSBET_MARKET_HISTORY", allocationSize = 1)
    private Long id;
    @Column(nullable = false) private Long variantId;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal priceAmount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, length = 40) private String quoteSource;
    @Column(nullable = false, length = 100) private String provider;
    @Column(nullable = false) private LocalDateTime quotedAt;
    @Column(nullable = false) private LocalDateTime fetchedAt;
    @Column(precision = 18, scale = 6) private BigDecimal originalAmount;
    @Column(length = 3) private String originalCurrency;
    @Column(precision = 18, scale = 8) private BigDecimal exchangeRate;
    private LocalDateTime exchangeRateAt;
    protected MarketHistory() { }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public String getOriginalCurrency() { return originalCurrency; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public LocalDateTime getExchangeRateAt() { return exchangeRateAt; }
    public MarketHistory(MarketVariant variant) {
        variantId = variant.getId(); priceAmount = variant.getPriceAmount(); currency = variant.getCurrency();
        originalAmount = variant.getOriginalAmount(); originalCurrency = variant.getOriginalCurrency();
        exchangeRate = variant.getExchangeRate(); exchangeRateAt = variant.getExchangeRateAt();
        quoteSource = variant.getQuoteSource(); provider = variant.getProvider(); quotedAt = variant.getQuotedAt(); fetchedAt = variant.getFetchedAt();
    }
    public BigDecimal getPriceAmount() { return priceAmount; }
    public String getCurrency() { return currency; }
    public String getQuoteSource() { return quoteSource; }
    public String getProvider() { return provider; }
    public LocalDateTime getQuotedAt() { return quotedAt; }
}
