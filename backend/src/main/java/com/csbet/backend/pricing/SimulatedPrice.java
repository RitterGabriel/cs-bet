package com.csbet.backend.pricing;

import com.csbet.backend.skin.CosmeticItem;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "CSBET_SIMULATED_PRICES", schema = "CSBET")
public class SimulatedPrice implements Persistable<Long> {
    // SKIN_ID é atribuído antes da inserção. ID não nulo não significa que a linha já existe.
    @Transient private boolean newEntity = true;
    @Id @Column(name = "SKIN_ID") private Long skinId;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SKIN_ID") private CosmeticItem skin;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal basePrice;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal currentPrice;
    @Column(nullable = false) private LocalDate priceDate;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @Column(nullable = false, length = 40) private String ruleVersion;
    protected SimulatedPrice() { }
    public SimulatedPrice(CosmeticItem skin, BigDecimal base, LocalDate day, LocalDateTime now) {
        if (skin == null || skin.getId() == null) {
            throw new IllegalArgumentException("O preço simulado exige uma skin já persistida no catálogo.");
        }
        this.skin = skin; this.skinId = skin.getId(); this.basePrice = base; this.currentPrice = base;
        this.priceDate = day; this.updatedAt = now; this.ruleVersion = PriceRules.VERSION;
    }
    public void update(BigDecimal amount, LocalDate day, LocalDateTime now) {
        this.currentPrice = amount; this.priceDate = day; this.updatedAt = now;
    }
    @Override public Long getId() { return skinId; }
    @Override public boolean isNew() { return newEntity; }

    @PostPersist
    @PostLoad
    private void markExisting() { newEntity = false; }

    public Long getSkinId() { return skinId; }
    public BigDecimal getBasePrice() { return basePrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public LocalDate getPriceDate() { return priceDate; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getRuleVersion() { return ruleVersion; }
}
