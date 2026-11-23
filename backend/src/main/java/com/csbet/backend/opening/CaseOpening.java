package com.csbet.backend.opening;

import com.csbet.backend.entity.User;
import com.csbet.backend.casecatalog.WeaponCase;
import com.csbet.backend.inventory.InventoryItem;
import com.csbet.backend.wallet.WalletMovement;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "CSBET_OPENINGS", schema = "CSBET")
public class CaseOpening {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "openingSequence")
    @SequenceGenerator(name = "openingSequence", sequenceName = "CSBET.SEQ_CSBET_OPENINGS", allocationSize = 1)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "USER_ID") private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "CASE_ID") private WeaponCase box;
    @Column(nullable = false, length = 36) private String requestId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "INVENTORY_ID") private InventoryItem inventory;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "WALLET_MOVEMENT_ID") private WalletMovement movement;
    @Column(precision = 18, scale = 2) private BigDecimal charged;
    @Column(precision = 18, scale = 2) private BigDecimal resultingBalance;
    @Column(precision = 18, scale = 2) private BigDecimal referencePrice;
    @Column(length = 1) private String special;
    private LocalDateTime createdAt;
    protected CaseOpening() { }
    CaseOpening(User user, WeaponCase box, String requestId, InventoryItem inventory, WalletMovement movement,
                BigDecimal charged, BigDecimal referencePrice, boolean special, Instant now) {
        this.user = user; this.box = box; this.requestId = requestId; this.inventory = inventory; this.movement = movement;
        this.charged = charged; this.resultingBalance = user.getBalance(); this.referencePrice = referencePrice;
        this.special = special ? "Y" : "N"; this.createdAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
    }
    public Long getId() { return id; }
    public WeaponCase getBox() { return box; }
    public InventoryItem getInventory() { return inventory; }
    public BigDecimal getCharged() { return charged; }
    public BigDecimal getResultingBalance() { return resultingBalance; }
    public BigDecimal getReferencePrice() { return referencePrice; }
    public boolean isSpecial() { return "Y".equals(special); }
}
