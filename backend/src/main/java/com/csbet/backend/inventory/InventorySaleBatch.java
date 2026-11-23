package com.csbet.backend.inventory;
import com.csbet.backend.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
@Entity
@Table(name = "CSBET_SALE_BATCHES", schema = "CSBET")
public class InventorySaleBatch {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "saleBatchSequence")
    @SequenceGenerator(name = "saleBatchSequence", sequenceName = "CSBET.SEQ_CSBET_SALE_BATCHES", allocationSize = 1)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "USER_ID") private User user;
    @Column(length = 36, nullable = false) private String requestId;
    @Column(length = 64, nullable = false) private String fingerprint;
    private int quantity;
    @Column(precision = 18, scale = 2) private BigDecimal amount;
    @Column(precision = 18, scale = 2) private BigDecimal resultingBalance;
    private LocalDateTime createdAt;
    protected InventorySaleBatch() { }
    InventorySaleBatch(User user, String requestId, String fingerprint, int quantity, BigDecimal amount, BigDecimal balance, Instant now) {
        this.user=user; this.requestId=requestId; this.fingerprint=fingerprint; this.quantity=quantity; this.amount=amount;
        resultingBalance=balance; createdAt=LocalDateTime.ofInstant(now,ZoneOffset.UTC);
    }
    boolean matches(String fingerprint) { return this.fingerprint.equals(fingerprint); }
    BulkSaleDtos.Receipt receipt() { return new BulkSaleDtos.Receipt(id,quantity,amount.setScale(2),resultingBalance.setScale(2)); }
}
