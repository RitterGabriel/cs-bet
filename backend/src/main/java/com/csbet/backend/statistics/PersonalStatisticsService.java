package com.csbet.backend.statistics;

import com.csbet.backend.inventory.InventoryRepository;
import com.csbet.backend.inventory.InventoryStatus;
import com.csbet.backend.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import static com.csbet.backend.statistics.PersonalStatisticsResponse.*;

@Service
public class PersonalStatisticsService {
    private final UserRepository users;
    private final StatisticsRepository statistics;
    private final InventoryRepository inventory;
    private final Clock clock;
    public PersonalStatisticsService(UserRepository users, StatisticsRepository statistics,
                                      InventoryRepository inventory, Clock pricingClock) {
        this.users = users; this.statistics = statistics; this.inventory = inventory; this.clock = pricingClock;
    }
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public PersonalStatisticsResponse read(Long ownerId, int page, int size, ActivityType type) {
        if (page < 0 || page > 100000 || size < 1 || size > 60 || type == null)
            throw new IllegalArgumentException("Filtros inválidos.");
        var user = users.findById(ownerId).orElseThrow(() -> new BadCredentialsException("Não autenticado."));
        var wallet = statistics.walletTotals(ownerId);
        var sales = statistics.salesTotals(ownerId);
        var valuation = inventory.estimateAvailable(ownerId, InventoryStatus.AVAILABLE,
                LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        // O motor estatístico não grava aberturas nem débitos: não inferir gastos a partir de cotações ou itens recebidos.
        var summary = new Summary(user.getBalance().setScale(2), wallet.added(), wallet.movements(),
                valuation.availableItems(), valuation.estimatedTotal(),
                valuation.unpricedItems() == null ? 0 : valuation.unpricedItems(), sales.count(),
                sales.unvalued() > 0 ? null : sales.total(), sales.unvalued(), false, null, null);
        long total = statistics.activityCount(ownerId, type);
        var activity = new ActivityPage(statistics.activity(ownerId, type, page, size), page, size,
                total, (int) ((total + size - 1) / size));
        return new PersonalStatisticsResponse(summary, activity);
    }
}
