package com.csbet.backend.pricing;

import com.csbet.backend.skin.CosmeticItemRepository;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/** Gerador legado da etapa 16; não é bean Spring e não é chamado pela API, CLI ou agendador. */
@Deprecated
public class PriceUpdateService {
    private final CosmeticItemRepository skins;
    private final SimulatedPriceRepository prices;
    private final PriceHistoryRepository history;
    private final PriceControlRepository control;
    private final Clock clock;
    public PriceUpdateService(CosmeticItemRepository skins, SimulatedPriceRepository prices,
                              PriceHistoryRepository history, PriceControlRepository control, Clock pricingClock) {
        this.skins = skins; this.prices = prices; this.history = history; this.control = control; this.clock = pricingClock;
    }
    public record UpdateResult(int initialized, int updated, LocalDate day) { }

    @Transactional
    public UpdateResult runDaily() {
        // O lock é mantido até COMMIT/ROLLBACK, inclusive entre diferentes instâncias da API.
        PriceControl state = control.lockForUpdate();
        if (state == null) throw new IllegalStateException("Controle de preços ausente. Aplique a migração V4.");
        Instant instant = clock.instant();
        LocalDate day = instant.atZone(clock.getZone()).toLocalDate();
        LocalDateTime now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        if (state.getLastRunDate() != null && state.getLastRunDate().isAfter(day)) {
            throw new IllegalStateException("Relógio anterior à última execução. Preços preservados.");
        }
        boolean dailyDue = !day.equals(state.getLastRunDate());
        Map<Long, SimulatedPrice> known = prices.findAll().stream().collect(Collectors.toMap(SimulatedPrice::getSkinId, Function.identity()));
        int initialized = 0, updated = 0;
        for (var skin : skins.findAllByOrderByIdAsc()) {
            var price = known.get(skin.getId());
            if (price == null) {
                price = prices.save(new SimulatedPrice(skin, PriceRules.base(skin.getRarity().getId()), day, now));
                history.save(new PriceHistory(price, "INITIAL"));
                initialized++;
            } else if (dailyDue && price.getPriceDate().isBefore(day)) {
                var amount = PriceRules.next(skin.getExternalId(), day, price.getCurrentPrice(), price.getBasePrice());
                // Se arredondamento/limite não mudar o valor, não cria histórico artificial.
                if (amount.compareTo(price.getCurrentPrice()) != 0) {
                    price.update(amount, day, now);
                    // findAll() carregou esta entidade na transação atual: dirty checking grava a alteração.
                    history.save(new PriceHistory(price, "DAILY"));
                    updated++;
                }
            }
        }
        prices.flush(); history.flush();
        state.complete(day);
        control.save(state);
        return new UpdateResult(initialized, updated, day);
    }
}
