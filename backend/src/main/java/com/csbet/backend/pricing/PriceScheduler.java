package com.csbet.backend.pricing;

import com.csbet.backend.market.MarketRefreshService;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "csbet.market.enabled", havingValue = "true", matchIfMissing = false)
public class PriceScheduler {
    private final MarketRefreshService service;
    public PriceScheduler(MarketRefreshService service) { this.service = service; }
    @Scheduled(fixedDelayString = "${csbet.market.poll-delay-ms:3600000}")
    public void update() {
        try { service.refresh(); }
        catch (Exception failure) {
            org.slf4j.LoggerFactory.getLogger(PriceScheduler.class).warn("Atualização de referências indisponível. Tipo: {}", failure.getClass().getSimpleName());
        }
    }
}
