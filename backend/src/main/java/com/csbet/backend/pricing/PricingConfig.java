package com.csbet.backend.pricing;
import java.time.*;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class PricingConfig {
    @Bean
    Clock pricingClock() { return Clock.system(ZoneId.of("America/Sao_Paulo")); }
}
