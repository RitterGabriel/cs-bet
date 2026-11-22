package com.csbet.backend.pricing;

import com.csbet.backend.skin.*;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.*;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PriceUpdateServiceTest {
    private final CosmeticItemRepository skins = mock(CosmeticItemRepository.class);
    private final SimulatedPriceRepository prices = mock(SimulatedPriceRepository.class);
    private final PriceHistoryRepository history = mock(PriceHistoryRepository.class);
    private final PriceControlRepository control = mock(PriceControlRepository.class);
    private final Map<Long, SimulatedPrice> priceStore = new HashMap<>();
    private final Map<String, PriceHistory> historyStore = new HashMap<>();
    private final PriceControl state = new PriceControl(1L);
    private final ReentrantLock rowLock = new ReentrantLock();
    private final MutableClock clock = new MutableClock();
    private PriceUpdateService service;
    private final List<CosmeticItem> catalog = new ArrayList<>();

    static class MutableClock extends Clock {
        private volatile Instant now = Instant.parse("2026-11-21T12:00:00Z");
        public ZoneId getZone() { return ZoneId.of("America/Sao_Paulo"); }
        public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        public Instant instant() { return now; }
        void nextDay() { now = now.plusSeconds(86400); }
    }
    private CosmeticItem skin(long id) {
        var skin = new CosmeticItem("BYMYKEL", "skin-fixture-" + id);
        skin.update("Fixture", "Fixture", "rifles", "none", null,
                new Rarity("rarity_rare_weapon", "Mil-Spec Grade", "#4b69ff"));
        ReflectionTestUtils.setField(skin, "id", id); return skin;
    }
    @BeforeEach
    void setup() {
        catalog.add(skin(1)); catalog.add(skin(2));
        // Dublê do lock da linha Oracle: serializa desde a leitura do controle até o fim da escrita.
        when(control.lockForUpdate()).thenAnswer(call -> { rowLock.lock(); return state; });
        when(control.save(any())).thenAnswer(call -> { rowLock.unlock(); return call.getArgument(0); });
        when(skins.findAllByOrderByIdAsc()).thenReturn(catalog);
        when(prices.findAll()).thenAnswer(call -> new ArrayList<>(priceStore.values()));
        when(prices.save(any())).thenAnswer(call -> { SimulatedPrice price = call.getArgument(0); priceStore.put(price.getSkinId(), price); return price; });
        when(history.save(any())).thenAnswer(call -> {
            PriceHistory point = call.getArgument(0);
            String key = point.getSkinId() + ":" + point.getPriceDate();
            if (historyStore.putIfAbsent(key, point) != null) throw new IllegalStateException("duplicate day");
            return point;
        });
        service = new PriceUpdateService(skins, prices, history, control, clock);
    }
    @Test
    void initializesBaseAndHistoryWithoutUpdatingAgainOnSameDay() {
        assertThat(service.runDaily().initialized()).isEqualTo(2);
        assertThat(service.runDaily().updated()).isZero();
        assertThat(priceStore).hasSize(2); assertThat(historyStore).hasSize(2);
        assertThat(priceStore.values()).allSatisfy(price -> assertThat(price.getCurrentPrice()).isEqualByComparingTo("100.00"));
        assertThat(historyStore.values()).allSatisfy(point -> assertThat(point.getEventType()).isEqualTo("INITIAL"));
        catalog.add(skin(3));
        assertThat(service.runDaily().initialized()).isEqualTo(1);
        assertThat(historyStore).hasSize(3);
    }
    @Test
    void advancesOncePerDayAndRecordsOnlyActualChanges() {
        service.runDaily();
        clock.nextDay();
        var result = service.runDaily();
        assertThat(result.updated()).isPositive();
        int count = historyStore.size();
        assertThat(count).isEqualTo(2 + result.updated());
        service.runDaily();
        assertThat(historyStore).hasSize(count);
        assertThat(historyStore.values()).anySatisfy(point -> assertThat(point.getEventType()).isEqualTo("DAILY"));
    }
    @Test
    void noPriceChangeDoesNotCreateHistoryButStillMarksTheDayProcessed() {
        service.runDaily();
        clock.nextDay();
        var floor = new BigDecimal("50.00");
        int attempts = 0;
        while (attempts++ < 366 && catalog.stream().anyMatch(skin -> PriceRules.next(skin.getExternalId(), LocalDate.now(clock), floor,
                new BigDecimal("100.00")).compareTo(floor) != 0)) clock.nextDay();
        priceStore.values().forEach(price -> price.update(floor, LocalDate.of(2026, 11, 21), LocalDateTime.of(2026, 11, 21, 12, 0)));
        int before = historyStore.size();
        assertThat(service.runDaily().updated()).isZero();
        assertThat(historyStore).hasSize(before);
        assertThat(state.getLastRunDate()).isEqualTo(LocalDate.now(clock));
        assertThat(service.runDaily().updated()).isZero();
    }

    @Test
    void concurrentWorkersInitializeAndUpdateOnlyOnce() throws Exception {
        assertThat(PriceControlRepository.class.getMethod("lockForUpdate").getAnnotation(Lock.class).value())
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            Callable<PriceUpdateService.UpdateResult> job = () -> { start.await(); return service.runDaily(); };
            var first = executor.submit(job); var second = executor.submit(job); start.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS).initialized() + second.get(5, TimeUnit.SECONDS).initialized()).isEqualTo(2);
            assertThat(historyStore).hasSize(2);
            clock.nextDay();
            var a = executor.submit(service::runDaily); var b = executor.submit(service::runDaily);
            int updated = a.get(5, TimeUnit.SECONDS).updated() + b.get(5, TimeUnit.SECONDS).updated();
            assertThat(historyStore).hasSize(2 + updated);
            assertThat(updated).isLessThanOrEqualTo(2);
        }
    }
    @Test
    void rulesAreDeterministicPositiveAndBoundedOverManyDays() {
        assertThat(PriceRules.base("rarity_ancient")).isEqualByComparingTo("25000.00");
        assertThat(PriceRules.base("unknown")).isEqualByComparingTo("100.00");
        BigDecimal base = new BigDecimal("100.00"), current = base;
        LocalDate day = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < 5000; i++) {
            var next = PriceRules.next("skin-fixture", day.plusDays(i), current, base);
            assertThat(next).isEqualTo(PriceRules.next("skin-fixture", day.plusDays(i), current, base));
            assertThat(next).isBetween(new BigDecimal("50.00"), new BigDecimal("150.00"));
            assertThat(next.subtract(current).abs()).isLessThanOrEqualTo(current.multiply(new BigDecimal("0.02")));
            assertThat(next.scale()).isEqualTo(2); current = next;
        }
    }
}
