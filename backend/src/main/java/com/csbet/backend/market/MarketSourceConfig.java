package com.csbet.backend.market;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
public class MarketSourceConfig {
    @Bean
    MarketPriceSource marketSource(Clock pricingClock, @Value("${csbet.market.source:market-csgo}") String selected) {
        if ("market-csgo".equals(selected)) return new MarketCsgoSource(new PublicMarketHttp(), new CatalogMarketIndex(), pricingClock);
        return new MarketPriceSource() {
            public boolean authorized() { return false; }
            public String provider() { return "NOT_CONFIGURED"; }
            public Duration refreshInterval() { return Duration.ofHours(1); }
            public List<Quote> fetch() { return List.of(); }
        };
    }
}
