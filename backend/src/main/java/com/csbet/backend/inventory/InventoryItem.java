package com.csbet.backend.inventory;

import com.csbet.backend.entity.User;
import com.csbet.backend.skin.CosmeticItem;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "CSBET_INVENTORY_ITEMS", schema = "CSBET")
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventorySequence")
    @SequenceGenerator(name = "inventorySequence", sequenceName = "CSBET.SEQ_CSBET_INVENTORY", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OWNER_ID", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SKIN_ID", nullable = false)
    private CosmeticItem skin;

    @Column(nullable = false)
    private LocalDateTime obtainedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InventoryStatus status;

    // Nulo para unidades antigas: desgaste/variante não podem ser inferidos a partir da skin.
    private Long marketVariantId;

    @Enumerated(EnumType.STRING)
    private com.csbet.backend.market.Wear wear;
    @Column(length = 1) private String statTrak;
    @Column(length = 120) private String phase;
    private LocalDateTime soldAt;

    // Snapshot do valor efetivo de uma futura venda, nunca o preço atual do catálogo.
    @Column(precision = 18, scale = 2)
    private BigDecimal salePrice;

    protected InventoryItem() { }

    public InventoryItem(User owner, CosmeticItem skin) {
        this.owner = Objects.requireNonNull(owner);
        this.skin = Objects.requireNonNull(skin);
        this.obtainedAt = LocalDateTime.now(Clock.systemUTC());
        this.status = InventoryStatus.AVAILABLE;
    }

    public InventoryItem(User owner, CosmeticItem skin, com.csbet.backend.market.MarketVariant variant, String phase) {
        this(owner, skin);
        if (!Objects.equals(skin.getId(), variant.getSkinId()) || variant.isSouvenir())
            throw new IllegalArgumentException("Variante incompatível.");
        this.marketVariantId = variant.getId();
        this.wear = variant.getWear(); this.statTrak = variant.isStatTrak() ? "Y" : "N"; this.phase = phase;
    }
    public com.csbet.backend.market.Wear getWear() { return wear; }
    public Boolean getStatTrak() { return statTrak == null ? null : "Y".equals(statTrak); }
    public String getPhase() { return phase; }
    public String getDisplayName() {
        return (Boolean.TRUE.equals(getStatTrak()) ? "StatTrak™ " : "") + skin.getName()
            + (phase == null ? "" : " · " + phase)
            + (wear == null || wear == com.csbet.backend.market.Wear.NONE ? "" : " (" + wear + ")");
    }
    public void sell(BigDecimal amount, LocalDateTime now) {
        if (status != InventoryStatus.AVAILABLE || amount == null || amount.signum() <= 0 || now.isBefore(obtainedAt))
            throw new IllegalArgumentException("Item indisponível para venda.");
        status = InventoryStatus.SOLD; salePrice = amount.setScale(2); soldAt = now;
    }
    public Long getMarketVariantId() { return marketVariantId; }
    public Long getId() { return id; }
    public CosmeticItem getSkin() { return skin; }
    public LocalDateTime getObtainedAt() { return obtainedAt; }
    public InventoryStatus getStatus() { return status; }
    public LocalDateTime getSoldAt() { return soldAt; }
    public BigDecimal getSalePrice() { return salePrice; }
}
