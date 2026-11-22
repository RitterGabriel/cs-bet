package com.csbet.backend.market;

import com.csbet.backend.casecatalog.*;
import com.csbet.backend.skin.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MarketRefreshTest {
    private final Instant now = Instant.parse("2026-11-21T12:00:00Z");
    private final MarketVariantRepository variants = mock(MarketVariantRepository.class);
    private final MarketHistoryRepository history = mock(MarketHistoryRepository.class);
    private final MarketControlRepository control = mock(MarketControlRepository.class);
    private final CosmeticItemRepository skins = mock(CosmeticItemRepository.class);
    private final WeaponCaseRepository cases = mock(WeaponCaseRepository.class);
    private final MarketControl state = new MarketControl(1L);
    private final List<MarketVariant> stored = new ArrayList<>();
    private final List<MarketHistory> observations = new ArrayList<>();
    private MarketRefreshTransactions transactions;

    private MarketPriceSource.Quote quote(MarketPriceSource.Target target, String externalId, String hash, Wear wear, String amount) {
        return new MarketPriceSource.Quote(target, externalId, hash, wear, false, false, new BigDecimal(amount), "BRL", now.minusSeconds(60));
    }
    @BeforeEach
    void setup() {
        var skin = new CosmeticItem("BYMYKEL", "skin-fixture"); ReflectionTestUtils.setField(skin, "id", 1L);
        var box = new WeaponCase("BYMYKEL", "crate-fixture"); ReflectionTestUtils.setField(box, "id", 2L);
        when(skins.findAllBySource("BYMYKEL")).thenReturn(List.of(skin));
        when(cases.findAllBySource("BYMYKEL")).thenReturn(List.of(box));
        when(control.lockForUpdate()).thenReturn(state);
        when(variants.findAll()).thenAnswer(call -> new ArrayList<>(stored));
        when(variants.save(any())).thenAnswer(call -> {
            MarketVariant variant = call.getArgument(0);
            if (variant.getId() == null) { ReflectionTestUtils.setField(variant, "id", (long) stored.size() + 1); stored.add(variant); }
            return variant;
        });
        when(history.save(any())).thenAnswer(call -> { observations.add(call.getArgument(0)); return call.getArgument(0); });
        transactions = new MarketRefreshTransactions(control, variants, history, skins, cases);
    }
    @Test
    void mapsExactWearAndCasesIndividuallyAndDoesNotDuplicateHistory() {
        var batch = List.of(quote(MarketPriceSource.Target.SKIN, "skin-fixture", "Fixture (Factory New)", Wear.FN, "100.00"),
                quote(MarketPriceSource.Target.SKIN, "skin-fixture", "Fixture (Field-Tested)", Wear.FT, "80.00"),
                quote(MarketPriceSource.Target.CASE, "crate-fixture", "Fixture Case", Wear.NONE, "5.25"));
        assertThat(transactions.apply(batch, "TEST_PROVIDER", now).stored()).isEqualTo(3);
        assertThat(transactions.apply(batch, "TEST_PROVIDER", now).stored()).isZero();
        assertThat(stored).hasSize(3); assertThat(observations).hasSize(3);
        assertThat(stored.get(0).getSkinId()).isEqualTo(1L);
        assertThat(stored.get(0).getWear()).isEqualTo(Wear.FN);
        assertThat(stored.get(1).getPriceAmount()).isEqualByComparingTo("80.00");
        assertThat(stored.get(2).getCaseId()).isEqualTo(2L);
        assertThat(observations).allSatisfy(point -> {
            assertThat(point.getCurrency()).isEqualTo("BRL");
            assertThat(point.getQuoteSource()).isEqualTo("STEAM_COMMUNITY_MARKET");
            assertThat(point.getProvider()).isEqualTo("TEST_PROVIDER");
        });
    }
    @Test
    void preservesUsdPtaxAndSourceInNewHistoryWithoutRewritingOldObservations() {
        var legacy = quote(MarketPriceSource.Target.CASE, "crate-fixture", "Fixture Case", Wear.NONE, "5.25");
        transactions.apply(List.of(legacy), "TEST_PROVIDER", now);
        var market = new MarketPriceSource.Quote(legacy.target(), legacy.externalId(), legacy.marketHashName(), Wear.NONE,
                false, false, new BigDecimal("5.50"), "BRL", now, "MARKET_CSGO", BigDecimal.ONE, "USD",
                new BigDecimal("5.50"), now.minusSeconds(600));
        assertThat(transactions.apply(List.of(market), "MARKET_CSGO_LOWEST_ASK_BCB_PTAX", now).stored()).isEqualTo(1);
        assertThat(observations).hasSize(2);
        assertThat(observations.get(0).getQuoteSource()).isEqualTo("STEAM_COMMUNITY_MARKET");
        assertThat(observations.get(0).getOriginalAmount()).isNull();
        var point = MarketDtos.HistoryPoint.from(observations.get(1));
        assertThat(point.source()).isEqualTo("MARKET_CSGO");
        assertThat(point.originalAmount()).isEqualByComparingTo("1");
        assertThat(point.originalCurrency()).isEqualTo("USD");
        assertThat(point.exchangeRate()).isEqualByComparingTo("5.50");
        assertThat(point.exchangeRateAt()).isNotNull();
        assertThat(transactions.apply(List.of(market), "MARKET_CSGO_LOWEST_ASK_BCB_PTAX", now).stored()).isZero();
        assertThat(observations).hasSize(2);
    }
    @Test
    void persistenceKeepsNormalStatTrakAndSouvenirPricesSeparateForTheSameWear() {
        var normal = quote(MarketPriceSource.Target.SKIN, "skin-fixture", "Fixture (Field-Tested)", Wear.FT, "10.00");
        var statTrak = new MarketPriceSource.Quote(normal.target(), normal.externalId(), "StatTrak™ Fixture (Field-Tested)", Wear.FT,
                true, false, new BigDecimal("20.00"), "BRL", normal.quotedAt());
        var souvenir = new MarketPriceSource.Quote(normal.target(), normal.externalId(), "Souvenir Fixture (Field-Tested)", Wear.FT,
                false, true, new BigDecimal("30.00"), "BRL", normal.quotedAt());
        assertThat(transactions.apply(List.of(normal, statTrak, souvenir), "TEST_PROVIDER", now).stored()).isEqualTo(3);
        assertThat(stored).hasSize(3); assertThat(observations).hasSize(3);
        assertThat(stored.get(0).getPriceAmount()).isEqualByComparingTo("10.00");
        assertThat(stored.get(1).isStatTrak()).isTrue(); assertThat(stored.get(1).getPriceAmount()).isEqualByComparingTo("20.00");
        assertThat(stored.get(2).isSouvenir()).isTrue(); assertThat(stored.get(2).getPriceAmount()).isEqualByComparingTo("30.00");
        var mismatched = new MarketPriceSource.Quote(normal.target(), normal.externalId(), statTrak.marketHashName(), Wear.FT,
                false, true, new BigDecimal("5.00"), "BRL", now);
        assertThat(transactions.apply(List.of(mismatched), "TEST_PROVIDER", now).rejected()).isEqualTo(1);
        assertThat(observations).hasSize(3);
        assertThat(stored.get(2).getPriceAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void invalidQuotesNeverOverwriteAnExistingReference() {
        var valid = quote(MarketPriceSource.Target.SKIN, "skin-fixture", "Fixture (Factory New)", Wear.FN, "100.00");
        transactions.apply(List.of(valid), "TEST_PROVIDER", now);
        var wrongWear = quote(MarketPriceSource.Target.SKIN, "skin-fixture", "Fixture (Factory New)", Wear.FT, "40.00");
        var badCurrency = new MarketPriceSource.Quote(valid.target(), valid.externalId(), valid.marketHashName(), valid.wear(), false, false, BigDecimal.ONE, "USD", now);
        var missing = quote(MarketPriceSource.Target.SKIN, "skin-missing", "Missing", Wear.FN, "20.00");
        var zero = quote(MarketPriceSource.Target.SKIN, "skin-fixture", "Fixture", Wear.FN, "0.00");
        var result = transactions.apply(List.of(wrongWear, badCurrency, missing, zero), "TEST_PROVIDER", now);
        assertThat(result.stored()).isZero(); assertThat(result.rejected()).isEqualTo(4);
        assertThat(stored.getFirst().getPriceAmount()).isEqualByComparingTo("100.00");
        assertThat(observations).hasSize(1);
    }
    @Test
    void claimsAreThrottledAndCommittedSeparatelyFromFetching() throws Exception {
        assertThat(transactions.claim(now, Duration.ofHours(6))).isTrue();
        assertThat(transactions.claim(now.plusSeconds(60), Duration.ofHours(6))).isFalse();
        assertThat(MarketControlRepository.class.getMethod("lockForUpdate").getAnnotation(org.springframework.data.jpa.repository.Lock.class).value())
                .isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        assertThat(MarketRefreshTransactions.class.getMethod("claim", Instant.class, Duration.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class).propagation())
                .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
    }
    @Test
    void expiredQuotesAreUnavailableAndAreNotReclassifiedAsSimulationsOrFreshPrices() {
        transactions.apply(List.of(quote(MarketPriceSource.Target.SKIN, "skin-fixture", "Fixture", Wear.FN, "100.00")), "TEST_PROVIDER", now);
        var fresh = MarketDtos.QuoteView.from(stored.getFirst(), now);
        var stale = MarketDtos.QuoteView.from(stored.getFirst(), now.plus(Duration.ofDays(2)));
        assertThat(fresh.status()).isEqualTo("AVAILABLE");
        assertThat(stale.status()).isEqualTo("STALE"); assertThat(stale.currentPrice()).isNull();
        assertThat(stale.source()).isEqualTo("STEAM_COMMUNITY_MARKET");
        assertThat(MarketDtos.QuoteView.unavailable().currentPrice()).isNull();
    }
    @Test
    void noAuthorizedSourceMeansNoNetworkOrDatabaseCallsAndFailurePreservesQuota() throws Exception {
        var source = mock(MarketPriceSource.class);
        var tx = mock(MarketRefreshTransactions.class);
        var service = new MarketRefreshService(source, tx, Clock.fixed(now, ZoneOffset.UTC), true);
        assertThat(service.refresh()).isEqualTo("DISABLED"); verifyNoInteractions(tx);
        when(source.authorized()).thenReturn(true); when(source.provider()).thenReturn("TEST_PROVIDER");
        when(source.refreshInterval()).thenReturn(Duration.ofHours(6)); when(tx.claim(any(), any())).thenReturn(true);
        when(source.fetch()).thenThrow(new java.io.IOException("private data"));
        assertThat(service.refresh()).isEqualTo("FAILED");
        verify(tx).finish(eq("FAILED"), any()); verify(tx, never()).apply(any(), any(), any());
    }
    @Test
    void hourlySchedulerRefetchesPersistsHistoryAndRecoversAfterFailureWithoutBrowserRequests() throws Exception {
        var source = mock(MarketPriceSource.class);
        when(source.authorized()).thenReturn(true); when(source.provider()).thenReturn("TEST_PROVIDER");
        when(source.refreshInterval()).thenReturn(Duration.ofHours(1));
        var clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);
        var first = quote(MarketPriceSource.Target.SKIN, "skin-fixture", "Fixture (Field-Tested)", Wear.FT, "80.00");
        var second = new MarketPriceSource.Quote(first.target(), first.externalId(), first.marketHashName(), first.wear(),
                false, false, new BigDecimal("81.00"), "BRL", now.plusSeconds(3600));
        when(source.fetch()).thenReturn(List.of(first)).thenReturn(List.of(second)).thenThrow(new java.io.IOException("offline"));
        var scheduler = new com.csbet.backend.pricing.PriceScheduler(new MarketRefreshService(source, transactions, clock, true));
        scheduler.update();
        assertThat(observations).hasSize(1);
        when(clock.instant()).thenReturn(now.plusSeconds(3599)); scheduler.update();
        verify(source, times(1)).fetch();
        when(clock.instant()).thenReturn(now.plusSeconds(3600)); scheduler.update();
        verify(source, times(2)).fetch(); assertThat(observations).hasSize(2);
        assertThat(stored.getFirst().getPriceAmount()).isEqualByComparingTo("81.00");
        assertThat(observations.getFirst().getPriceAmount()).isEqualByComparingTo("80.00");
        when(clock.instant()).thenReturn(now.plusSeconds(7200)); scheduler.update();
        assertThat(state.getLastStatus()).isEqualTo("FAILED"); assertThat(observations).hasSize(2);
        assertThat(stored.getFirst().getPriceAmount()).isEqualByComparingTo("81.00");
        doReturn(List.of(new MarketPriceSource.Quote(first.target(), first.externalId(), first.marketHashName(), first.wear(),
                false, false, new BigDecimal("82.00"), "BRL", now.plusSeconds(10800)))).when(source).fetch();
        when(clock.instant()).thenReturn(now.plusSeconds(10800)); scheduler.update();
        assertThat(observations).hasSize(3); assertThat(state.getLastStatus()).isEqualTo("SUCCESS");
        assertThat(stored.getFirst().getPriceAmount()).isEqualByComparingTo("82.00");
    }

    @Test
    void disabledRefreshDoesNotQueryTheSourceOrOracle() {
        var source = mock(MarketPriceSource.class); var tx = mock(MarketRefreshTransactions.class);
        assertThat(new MarketRefreshService(source, tx, Clock.fixed(now, ZoneOffset.UTC), false).refresh()).isEqualTo("DISABLED");
        verifyNoInteractions(source, tx);
    }

    @Test
    void aCompletelyInvalidBatchIsNotRecordedAsASuccessfulRefresh() throws Exception {
        var source = mock(MarketPriceSource.class);
        var tx = mock(MarketRefreshTransactions.class);
        when(source.authorized()).thenReturn(true); when(source.provider()).thenReturn("TEST_PROVIDER");
        when(source.refreshInterval()).thenReturn(Duration.ofHours(6)); when(tx.claim(any(), any())).thenReturn(true);
        when(source.fetch()).thenReturn(List.of(quote(MarketPriceSource.Target.CASE, "crate-fixture", "Fixture Case", Wear.NONE, "0.00")));
        when(tx.apply(any(), any(), any())).thenReturn(new MarketRefreshTransactions.Applied(0, 1));
        var service = new MarketRefreshService(source, tx, Clock.fixed(now, ZoneOffset.UTC), true);
        assertThat(service.refresh()).isEqualTo("REJECTED");
        verify(tx).finish(eq("REJECTED"), any());
    }

}
