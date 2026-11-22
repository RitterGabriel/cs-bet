package com.csbet.backend.market;

import java.io.IOException;
import java.math.*;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import tools.jackson.databind.JsonNode;

/** Fontes públicas documentadas (sem login/chave):
 * https://market.csgo.com/pt/api/content/start — menor anúncio, USD (não é preço de venda realizada).
 * https://dadosabertos.bcb.gov.br/dataset/dolar-americano-usd-todos-os-boletins-diarios — PTAX venda.
 * Uma tabela completa por hora; 429/falhas aguardam o próximo intervalo, sem contornar bloqueios.
 */
public final class MarketCsgoSource implements MarketPriceSource {
    static final URI PRICES = URI.create("https://market.csgo.com/api/v2/prices/USD.json");
    private static final ZoneId BCB_ZONE = ZoneId.of("America/Sao_Paulo");
    private final MarketJsonClient http;
    private final CatalogMarketIndex catalog;
    private final Clock clock;
    MarketCsgoSource(MarketJsonClient http, CatalogMarketIndex catalog, Clock clock) {
        this.http = http; this.catalog = catalog; this.clock = clock;
    }
    public boolean authorized() { return true; }
    public String provider() { return "MARKET_CSGO_LOWEST_ASK_BCB_PTAX"; }
    public Duration refreshInterval() { return Duration.ofHours(1); }
    public List<Quote> fetch() throws Exception {
        Instant now = clock.instant();
        JsonNode prices = http.get(PRICES);
        if (!prices.path("success").asBoolean(false) || !"USD".equals(prices.path("currency").asText())
                || !prices.path("items").isArray() || prices.path("items").size() > 100000)
            throw new IOException("Tabela de preços inválida.");
        Instant quotedAt = Instant.ofEpochSecond(prices.path("time").asLong(0));
        if (quotedAt.isAfter(now) || !quotedAt.plus(Duration.ofHours(24)).isAfter(now))
            throw new IOException("Tabela de preços sem data recente.");
        var fx = exchangeRate(http.get(exchangeUri(now)), now);
        List<Quote> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new HashSet<>();
        for (var item : prices.path("items")) {
            String hash = item.path("market_hash_name").asText("");
            if (!seen.add(hash)) duplicates.add(hash);
            var match = catalog.match(hash);
            if (match == null || item.path("volume").asLong(0) <= 0) continue;
            try {
                BigDecimal usd = new BigDecimal(item.path("price").asText(""));
                if (usd.signum() <= 0 || usd.setScale(6, RoundingMode.UNNECESSARY).precision() > 18) continue;
                BigDecimal brl = usd.multiply(fx.rate()).setScale(2, RoundingMode.HALF_UP);
                var quote = new Quote(match.target(), match.externalId(), hash, match.wear(), match.statTrak(), match.souvenir(),
                        brl, "BRL", quotedAt, "MARKET_CSGO", usd, "USD", fx.rate(), fx.at());
                if (MarketRefreshTransactions.valid(quote, now)) result.add(quote);
            } catch (NumberFormatException | ArithmeticException invalid) { /* Não substitui preços válidos por registros malformados. */ }
        }
        result.removeIf(quote -> duplicates.contains(quote.marketHashName()));
        if (result.isEmpty()) throw new IOException("Nenhuma referência válida e identificável no catálogo.");
        return List.copyOf(result);
    }
    record Exchange(BigDecimal rate, Instant at) { }
    static Exchange exchangeRate(JsonNode data, Instant now) throws IOException {
        var values = data.path("value");
        if (!values.isArray() || values.size() != 1) throw new IOException("Cotação PTAX indisponível.");
        try {
            var value = values.get(0);
            BigDecimal rate = new BigDecimal(value.path("cotacaoVenda").asText(""));
            Instant at = LocalDateTime.parse(value.path("dataHoraCotacao").asText("").replace(' ', 'T')).atZone(BCB_ZONE).toInstant();
            if (rate.signum() <= 0 || rate.setScale(8, RoundingMode.UNNECESSARY).precision() > 18
                    || at.isAfter(now) || !at.plus(Duration.ofDays(10)).isAfter(now)) throw new IllegalArgumentException();
            return new Exchange(rate, at);
        } catch (RuntimeException invalid) { throw new IOException("Cotação PTAX inválida."); }
    }
    static URI exchangeUri(Instant now) {
        var end = now.atZone(BCB_ZONE).toLocalDate();
        var format = DateTimeFormatter.ofPattern("MM-dd-uuuu");
        return URI.create("https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/"
                + "CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)"
                + "?@dataInicial='" + end.minusDays(10).format(format) + "'&@dataFinalCotacao='" + end.format(format)
                + "'&$top=1&$orderby=dataHoraCotacao%20desc&$format=json");
    }
}
