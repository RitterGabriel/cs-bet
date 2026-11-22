package com.csbet.backend.market;

import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MarketRefreshService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MarketRefreshService.class);
    private final MarketPriceSource source;
    private final MarketRefreshTransactions transactions;
    private final Clock clock;
    private final boolean enabled;
    public MarketRefreshService(MarketPriceSource source, MarketRefreshTransactions transactions, Clock pricingClock,
                               @Value("${csbet.market.enabled:false}") boolean enabled) {
        this.source = source; this.transactions = transactions; this.clock = pricingClock; this.enabled = enabled;
    }
    public String refresh() {
        if (!enabled || !source.authorized()) return "DISABLED";
        Duration interval = source.refreshInterval();
        if (interval == null || interval.compareTo(Duration.ofMinutes(1)) < 0
                || source.provider() == null || source.provider().isBlank() || source.provider().length() > 100) return "INVALID_CONFIG";
        if (!transactions.claim(clock.instant(), interval)) return "THROTTLED";
        try {
            var quotes = source.fetch();
            if (quotes == null || quotes.isEmpty() || quotes.size() > 20000) throw new IllegalArgumentException("Lote inválido.");
            var result = transactions.apply(quotes, source.provider(), clock.instant());
            String status = result.rejected() == 0 ? "SUCCESS" : result.stored() > 0 ? "PARTIAL" : "REJECTED";
            if (result.rejected() > 0) log.warn("{} cotações inválidas rejeitadas; referências anteriores preservadas.", result.rejected());
            transactions.finish(status, clock.instant());
            log.info("Referências de mercado: status={}, gravadas={}, rejeitadas={}, fonte={}.",
                    status, result.stored(), result.rejected(), source.provider());
            return status;
        } catch (Exception failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            transactions.finish("FAILED", clock.instant());
            log.warn("Consulta de cotações falhou; referências anteriores foram preservadas. Tipo: {}", failure.getClass().getSimpleName());
            return "FAILED";
        }
    }
}
