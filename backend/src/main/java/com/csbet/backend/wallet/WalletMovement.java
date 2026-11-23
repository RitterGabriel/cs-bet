package com.csbet.backend.wallet;

import com.csbet.backend.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "CSBET_WALLET_MOVEMENTS")
public class WalletMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "walletSequence")
    @SequenceGenerator(name = "walletSequence", sequenceName = "SEQ_CSBET_WALLET_MOVEMENTS", allocationSize = 1)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;
    @Column(nullable = false, length = 20)
    private String type;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal resultingBalance;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false, length = 36)
    private String requestId;

    protected WalletMovement() { }
    public WalletMovement(User user, BigDecimal amount, String requestId) {
        this.user = user;
        this.type = "CREDIT_ADDITION";
        this.amount = amount;
        this.resultingBalance = user.getBalance();
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        this.requestId = requestId;
    }
    public static WalletMovement opening(User user, BigDecimal amount, String requestId) {
        var movement = new WalletMovement(user, amount, requestId);
        movement.type = "CASE_OPENING";
        return movement;
    }
    public static WalletMovement sale(User user, BigDecimal amount, String requestId) {
        var movement = new WalletMovement(user, amount, requestId);
        movement.type = "ITEM_SALE";
        return movement;
    }
    public Long getId() { return id; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getResultingBalance() { return resultingBalance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
