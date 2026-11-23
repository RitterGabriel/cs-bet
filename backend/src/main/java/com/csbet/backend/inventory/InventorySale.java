package com.csbet.backend.inventory;
import com.csbet.backend.entity.User;
import com.csbet.backend.wallet.WalletMovement;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
@Entity
@Table(name = "CSBET_INVENTORY_SALES", schema = "CSBET")
public class InventorySale {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "saleSequence")
    @SequenceGenerator(name = "saleSequence", sequenceName = "CSBET.SEQ_CSBET_INVENTORY_SALES", allocationSize = 1)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "USER_ID") private User user;
    @Column(length = 36, nullable = false) private String requestId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "INVENTORY_ID") private InventoryItem inventory;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "WALLET_MOVEMENT_ID") private WalletMovement movement;
    @Column(precision = 18, scale = 2) private BigDecimal amount;
    @Column(precision = 18, scale = 2) private BigDecimal resultingBalance;
    private LocalDateTime createdAt;
    protected InventorySale() { }
    InventorySale(User user, InventoryItem item, InventorySaleRequest request, WalletMovement movement, Instant now) {
        this.user = user; inventory = item; requestId = request.requestId().toString(); this.movement = movement;
        amount = item.getSalePrice(); resultingBalance = user.getBalance(); createdAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
    }
    public Long getId() { return id; }
    public Long getInventoryId() { return inventory.getId(); }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getResultingBalance() { return resultingBalance; }
}
