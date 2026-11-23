package com.csbet.backend.opening;

import com.csbet.backend.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name = "CSBET_OPENING_BATCHES", schema = "CSBET")
public class OpeningBatch {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "batchSequence")
    @SequenceGenerator(name = "batchSequence", sequenceName = "CSBET.SEQ_CSBET_OPENING_BATCHES", allocationSize = 1)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "USER_ID") private User user;
    @Column(nullable = false, length = 36) private String requestId;
    @Column(nullable = false, length = 120) private String caseExternalId;
    private int quantity;
    @Column(precision = 18, scale = 2) private BigDecimal unitPrice;
    private LocalDateTime createdAt;
    protected OpeningBatch() { }
    OpeningBatch(User user, OpeningBatchRequest request, Instant now) {
        this.user = user; requestId = request.requestId().toString(); caseExternalId = request.caseId();
        quantity = request.quantity(); unitPrice = request.expectedPrice(); createdAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
    }
    public Long getId() { return id; }
    boolean matches(OpeningBatchRequest request) {
        return caseExternalId.equals(request.caseId()) && quantity == request.quantity() && unitPrice.compareTo(request.expectedPrice()) == 0;
    }
}
