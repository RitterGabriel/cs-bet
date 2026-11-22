package com.csbet.backend.pricing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "CSBET_PRICE_HISTORY", schema = "CSBET")
public class PriceHistory {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "priceHistorySequence")
    @SequenceGenerator(name = "priceHistorySequence", sequenceName = "CSBET.SEQ_CSBET_PRICE_HISTORY", allocationSize = 1)
    private Long id;
    @Column(nullable = false) private Long skinId;
    @Column(nullable = false) private LocalDate priceDate;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @Column(nullable = false) private LocalDateTime recordedAt;
    @Column(nullable = false, length = 16) private String eventType;
    protected PriceHistory() { }
    public PriceHistory(SimulatedPrice price, String eventType) {
        this.skinId = price.getSkinId(); this.priceDate = price.getPriceDate();
        this.amount = price.getCurrentPrice(); this.recordedAt = price.getUpdatedAt(); this.eventType = eventType;
    }
    public Long getSkinId() { return skinId; }
    public LocalDate getPriceDate() { return priceDate; }
    public BigDecimal getAmount() { return amount; }
    public String getEventType() { return eventType; }
}
