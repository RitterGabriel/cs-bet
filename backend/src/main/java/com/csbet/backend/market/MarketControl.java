package com.csbet.backend.market;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CSBET_MARKET_CONTROL", schema = "CSBET")
public class MarketControl {
    @Id private Long id;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime lastSuccessAt;
    @Column(length = 24) private String lastStatus;
    protected MarketControl() { }
    public MarketControl(Long id) { this.id = id; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public String getLastStatus() { return lastStatus; }
    public void attempt(LocalDateTime now, LocalDateTime next) { lastAttemptAt = now; nextAttemptAt = next; }
    public void finish(String status, LocalDateTime now) { lastStatus = status; if (status.equals("SUCCESS") || status.equals("PARTIAL")) lastSuccessAt = now; }
}
