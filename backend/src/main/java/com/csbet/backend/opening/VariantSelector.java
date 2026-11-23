package com.csbet.backend.opening;

import com.csbet.backend.market.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/** Regra interna: desgaste uniforme, depois StatTrak uniforme entre as opções cadastradas.
 * Identidades de variantes são usadas inclusive com cotação vencida; preço não participa do sorteio. */
@Component
public class VariantSelector {
    public static final String RULE = "CSBET_UNIFORM_WEAR_THEN_STATTRAK_V1";
    private final RandomGenerator random;
    public VariantSelector() { this(new SecureRandom()); }
    VariantSelector(RandomGenerator random) { this.random = random; }
    MarketVariant select(List<MarketVariant> variants, Long skinId, boolean glove) {
        var pool = variants.stream().filter(v -> Objects.equals(v.getSkinId(), skinId)
                && !v.isSouvenir() && (!glove || !v.isStatTrak()))
                .sorted(Comparator.comparing(MarketVariant::getId)).toList();
        var wears = pool.stream().map(MarketVariant::getWear).distinct().sorted().toList();
        if (wears.isEmpty()) throw new OpeningUnavailableException("Esta skin ainda não tem variantes cadastradas. Nenhum crédito foi consumido.");
        var wear = wears.get(random.nextInt(wears.size()));
        var options = pool.stream().filter(v -> v.getWear() == wear)
                .collect(java.util.stream.Collectors.toMap(MarketVariant::isStatTrak, v -> v, (first, ignored) -> first));
        var flags = options.keySet().stream().sorted().toList();
        return options.get(flags.get(random.nextInt(flags.size())));
    }
}
