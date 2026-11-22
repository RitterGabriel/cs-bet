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

    public Long getId() { return id; }
    public CosmeticItem getSkin() { return skin; }
    public LocalDateTime getObtainedAt() { return obtainedAt; }
    public InventoryStatus getStatus() { return status; }
    public LocalDateTime getSoldAt() { return soldAt; }
    public BigDecimal getSalePrice() { return salePrice; }
}
