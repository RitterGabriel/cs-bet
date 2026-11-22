package com.csbet.backend.pricing;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "CSBET_PRICE_CONTROL", schema = "CSBET")
public class PriceControl {
    @Id private Long id;
    private LocalDate lastRunDate;
    protected PriceControl() { }
    public PriceControl(Long id) { this.id = id; }
    public LocalDate getLastRunDate() { return lastRunDate; }
    public void complete(LocalDate date) { this.lastRunDate = date; }
}
