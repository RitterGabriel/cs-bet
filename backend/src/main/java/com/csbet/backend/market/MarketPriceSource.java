package com.csbet.backend.market;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

/**
 * SPI para um adaptador cuja autorização e limites tenham sido verificados.
 * 2026-11-21: ISteamEconomy/GetMarketPrices exige publisher key; IEconMarketService é restrito.
 * https://partner.steamgames.com/doc/webapi/ISteamEconomy
 * https://partner.steamgames.com/doc/webapi/IEconMarketService
 * Os 100.000 pedidos/dia de https://steamcommunity.com/dev/apiterms não são uma quota
 * documentada de /market/priceoverview. Essa rota não é utilizada aqui.
 * GetAssetPrices documenta compras de itens; não equivale a cotações de revenda do Mercado.
 * BRL existe no ecossistema Steam, mas não há aqui um endpoint autorizado de cotações BRL.
 * Hashes devem vir de mapeamento verificado, incluindo desgaste/StatTrak/Souvenir;
 * nunca gerar variantes apenas concatenando nomes do catálogo. Não usar cookies privados.
 */
public interface MarketPriceSource {
    enum Target { SKIN, CASE }
    record Quote(Target target, String externalId, String marketHashName, Wear wear,
                 boolean statTrak, boolean souvenir, BigDecimal amount, String currency, Instant quotedAt,
                 String source, BigDecimal originalAmount, String originalCurrency, BigDecimal exchangeRate, Instant exchangeRateAt) {
        public Quote(Target target, String externalId, String marketHashName, Wear wear,
                     boolean statTrak, boolean souvenir, BigDecimal amount, String currency, Instant quotedAt) {
            this(target, externalId, marketHashName, wear, statTrak, souvenir, amount, currency, quotedAt,
                    "STEAM_COMMUNITY_MARKET", null, null, null, null);
        }
    }
    boolean authorized();
    String provider();
    // O adaptador deve respeitar a quota real da fonte e impor timeout e tamanho máximo de resposta.
    Duration refreshInterval();
    List<Quote> fetch() throws Exception;
}
