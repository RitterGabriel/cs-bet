package com.csbet.backend.market;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class MarketCsgoSourceTest {
    private static final Instant NOW = Instant.parse("2026-11-21T12:00:00Z");
    private final JsonMapper mapper = new JsonMapper();
    private final CatalogMarketIndex catalog = new CatalogMarketIndex();
    private String feed(String items) {
        return "{\"success\":true,\"currency\":\"USD\",\"time\":" + NOW.minusSeconds(60).getEpochSecond() + ",\"items\":[" + items + "]}";
    }
    private String item(String name, String price) {
        return "{\"market_hash_name\":\"" + name + "\",\"price\":\"" + price + "\",\"volume\":10}";
    }
    private static final String FX = "{\"value\":[{\"cotacaoVenda\":5.8889,\"dataHoraCotacao\":\"2026-11-19 13:04:18.325373\"}]}";
    private MarketCsgoSource source(String data, String fx, List<URI> requests) {
        return new MarketCsgoSource(uri -> { requests.add(uri); return mapper.readTree(uri.equals(MarketCsgoSource.PRICES) ? data : fx); }, catalog, Clock.fixed(NOW, ZoneOffset.UTC));
    }
    @Test
    void mapsFeverCaseAndIndividualSkinWearInBrlWithoutCredentials() throws Exception {
        List<URI> requests = new ArrayList<>();
        var quotes = source(feed(item("Fever Case", "0.479") + "," + item("AK-47 | Redline (Field-Tested)", "24.846")
                + "," + item("StatTrak™ AK-47 | Redline (Minimal Wear)", "100")), FX, requests).fetch();
        assertThat(quotes).hasSize(3);
        var box = quotes.get(0);
        assertThat(box.target()).isEqualTo(MarketPriceSource.Target.CASE);
        assertThat(box.externalId()).startsWith("crate-");
        assertThat(box.wear()).isEqualTo(Wear.NONE);
        assertThat(box.amount()).isEqualByComparingTo("2.82");
        assertThat(box.originalAmount()).isEqualByComparingTo("0.479");
        assertThat(box.exchangeRate()).isEqualByComparingTo("5.8889");
        assertThat(box.exchangeRateAt()).isEqualTo(Instant.parse("2026-11-19T16:04:18.325373Z"));
        assertThat(quotes.get(1).wear()).isEqualTo(Wear.FT);
        assertThat(quotes.get(1).amount()).isEqualByComparingTo("146.32");
        assertThat(quotes.get(2).statTrak()).isTrue();
        assertThat(quotes.get(2).wear()).isEqualTo(Wear.MW);
        assertThat(quotes).allSatisfy(q -> {
            assertThat(q.currency()).isEqualTo("BRL"); assertThat(q.originalCurrency()).isEqualTo("USD");
            assertThat(q.source()).isEqualTo("MARKET_CSGO"); assertThat(MarketRefreshTransactions.valid(q, NOW)).isTrue();
        });
        assertThat(requests).hasSize(2);
        assertThat(requests.get(1).toString()).contains("$top=1", "dataHoraCotacao%20desc");
    }
    @Test
    void allFiveWearsStatTrakAndSouvenirResolveToTheSameBaseSkinWithoutMixingVariants() throws Exception {
        List<String> entries = new ArrayList<>();
        var wears = Map.of("Factory New", Wear.FN, "Minimal Wear", Wear.MW, "Field-Tested", Wear.FT,
                "Well-Worn", Wear.WW, "Battle-Scarred", Wear.BS);
        for (String wear : wears.keySet()) {
            entries.add(item("AK-47 | Redline (" + wear + ")", "10"));
            entries.add(item("StatTrak™ AK-47 | Redline (" + wear + ")", "20"));
            entries.add(item("Souvenir AK-47 | Redline (" + wear + ")", "30"));
        }
        // Feed sintético para cobrir o protocolo; não afirma disponibilidade dessas combinações no mercado.
        var quotes = source(feed(String.join(",", entries)), FX, new ArrayList<>()).fetch();
        assertThat(quotes).hasSize(15);
        assertThat(quotes.stream().map(MarketPriceSource.Quote::externalId).distinct()).hasSize(1);
        for (var wear : wears.values()) {
            var group = quotes.stream().filter(q -> q.wear() == wear).toList();
            assertThat(group).hasSize(3);
            assertThat(group.stream().filter(MarketPriceSource.Quote::statTrak)).hasSize(1);
            assertThat(group.stream().filter(MarketPriceSource.Quote::souvenir)).hasSize(1);
            assertThat(group).allSatisfy(q -> assertThat(q.statTrak() && q.souvenir()).isFalse());
        }
    }
    @Test
    void weekendKeepsLatestBusinessDayPtaxButStillFetchesNewMarketPrices() throws Exception {
        var requests = new ArrayList<URI>();
        var clock = org.mockito.Mockito.mock(Clock.class);
        org.mockito.Mockito.when(clock.instant()).thenReturn(NOW); // sábado; PTAX anterior de quinta-feira
        var source = new MarketCsgoSource(uri -> {
            requests.add(uri);
            return mapper.readTree(uri.equals(MarketCsgoSource.PRICES)
                    ? "{\"success\":true,\"currency\":\"USD\",\"time\":" + clock.instant().minusSeconds(60).getEpochSecond()
                      + ",\"items\":[" + item("Fever Case", requests.size() <= 2 ? "1" : "2") + "]}"
                    : FX);
        }, catalog, clock);
        var first = source.fetch().getFirst();
        org.mockito.Mockito.when(clock.instant()).thenReturn(NOW.plusSeconds(3600));
        var next = source.fetch().getFirst();
        assertThat(requests).hasSize(4);
        assertThat(next.quotedAt()).isAfter(first.quotedAt());
        assertThat(next.exchangeRateAt()).isEqualTo(first.exchangeRateAt());
        assertThat(next.exchangeRate()).isEqualByComparingTo(first.exchangeRate());
        assertThat(next.amount()).isEqualByComparingTo(first.amount().multiply(new BigDecimal("2")));
    }
    @Test
    void everyOfficialCaseInTheSnapshotCanReceiveItsOwnQuote() throws Exception {
        try (var input = getClass().getResourceAsStream("/catalog/crates.json")) {
            var cases = mapper.readTree(input);
            List<String> entries = new ArrayList<>();
            for (var box : cases) entries.add(item(box.path("name").asText(), "1.00"));
            var quotes = source(feed(String.join(",", entries)), FX, new ArrayList<>()).fetch();
            assertThat(quotes).hasSize(42).allSatisfy(q -> {
                assertThat(q.target()).isEqualTo(MarketPriceSource.Target.CASE);
                assertThat(q.wear()).isEqualTo(Wear.NONE);
            });
            assertThat(quotes.stream().map(MarketPriceSource.Quote::externalId).distinct()).hasSize(42);
        }
    }
    @Test
    void rejectsUnknownOrAmbiguousPhasesAndDoesNotInventWear() {
        assertThat(catalog.match("★ Bayonet | Doppler (Factory New)")).isNull();
        assertThat(catalog.match("Unknown Case")).isNull();
        assertThat(catalog.match("AK-47 | Redline (Imaginary Wear)")).isNull();
        var knife = catalog.match("★ StatTrak™ Bayonet | Fade (Factory New)");
        assertThat(knife).isNotNull(); assertThat(knife.statTrak()).isTrue(); assertThat(knife.wear()).isEqualTo(Wear.FN);
        assertThat(catalog.match("Souvenir AK-47 | Redline (Field-Tested)").souvenir()).isTrue();
    }
    @Test
    void ignoresInvalidAmountsOutOfStockAndDuplicatesWithoutFallbackPrices() throws Exception {
        var data = feed(item("Fever Case", "0.479") + "," + item("Revolution Case", "-1") + ","
                + item("Fracture Case", "bad") + "," + item("Clutch Case", "0") + ","
                + item("Spectrum Case", "1") + "," + item("Spectrum Case", "2") + ","
                + item("Snakebite Case", "1").replace("\"volume\":10", "\"volume\":0"));
        assertThat(source(data, FX, new ArrayList<>()).fetch()).extracting(MarketPriceSource.Quote::marketHashName).containsExactly("Fever Case");
    }
    @Test
    void invalidOrStaleMarketOrFxDataFailsTheBatch() {
        var valid = feed(item("Fever Case", "1"));
        for (String data : List.of("{}", valid.replace("\"USD\"", "\"BRL\""), valid.replace("true", "false"), valid.replace("" + NOW.minusSeconds(60).getEpochSecond(), "1"))) {
            assertThatThrownBy(() -> source(data, FX, new ArrayList<>()).fetch()).isInstanceOf(IOException.class);
        }
        for (String fx : List.of("{}", "{\"value\":[]}", FX.replace("5.8889", "0"), FX.replace("2026-11-19", "2026-10-01"), FX.replace("2026-11-19", "2026-12-01"))) {
            assertThatThrownBy(() -> source(valid, fx, new ArrayList<>()).fetch()).isInstanceOf(IOException.class);
        }
    }
    @Test
    void externalFailureIsNotRetriedAndNetworkIntervalIsOneHour() {
        List<URI> requests = new ArrayList<>();
        var source = new MarketCsgoSource(uri -> { requests.add(uri); throw new IOException("HTTP 429"); }, catalog, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(source::fetch).isInstanceOf(IOException.class);
        assertThat(requests).containsExactly(MarketCsgoSource.PRICES);
        assertThat(source.refreshInterval()).isEqualTo(Duration.ofHours(1));
    }
    @Test
    void neverConnectsToUnapprovedHostsOrPlainHttp() {
        for (String url : List.of("http://market.csgo.com/api/v2/prices/USD.json", "https://example.com/feed", "https://user@market.csgo.com/feed")) {
            assertThatThrownBy(() -> new PublicMarketHttp().get(URI.create(url))).isInstanceOf(IOException.class);
        }
    }
}
