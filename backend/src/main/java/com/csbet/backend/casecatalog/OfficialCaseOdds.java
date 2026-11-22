package com.csbet.backend.casecatalog;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Publicação oficial consultada em 2026-11-22:
 * https://www.csgo.com.cn/news/gamebroad/20170911/206155.shtml
 * Especial:Covert = 2:5; demais níveis consecutivos = 1:5;
 * itens da mesma qualidade têm chances iguais. Pesos: 625:125:25:5:2 (total 782).
 * Expõe chances das skins comuns e do grupo especial separadamente.
 * A seleção uniforme dentro do grupo é uma regra própria do CS Bet, não uma chance oficial por fase.
 */
@Component
public class OfficialCaseOdds {
    public static final String SOURCE = "https://www.csgo.com.cn/news/gamebroad/20170911/206155.shtml";
    private static final Map<String,Integer> WEIGHTS = Map.of(
            "rarity_rare_weapon",625, "rarity_mythical_weapon",125,
            "rarity_legendary_weapon",25, "rarity_ancient_weapon",5);
    private final Map<String,Map<String,String>> verifiedCases;
    private final Map<String,Map<String,String>> catalogSpecialItems;
    private final String revision;

    public record Chance(@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal percent,
                         String basis, String scope, String sourceUrl) { }

    public OfficialCaseOdds() { this(new CatalogSourceLoader().bundled()); }
    OfficialCaseOdds(SourceCatalog catalog) {
        revision = catalog.revision();
        var rarities = catalog.items().stream().collect(Collectors.toMap(SourceCatalog.SourceItem::id,item -> item.rarity().id()));
        Map<String,Map<String,String>> cases = new HashMap<>();
        for (var box : catalog.cases()) {
            if (box.links().stream().noneMatch(SourceCatalog.SourceLink::special)) continue;
            Map<String,String> normal = new HashMap<>();
            box.links().stream().filter(link -> !link.special()).forEach(link -> normal.put(link.itemId(),rarities.get(link.itemId())));
            if (new HashSet<>(normal.values()).equals(WEIGHTS.keySet())) cases.put(box.id(),Map.copyOf(normal));
        }
        verifiedCases = Map.copyOf(cases);
        Map<String,Map<String,String>> specialItems = new HashMap<>();
        for (var box : catalog.cases()) {
            Map<String,String> entries = new HashMap<>();
            box.links().stream().filter(SourceCatalog.SourceLink::special)
                    .forEach(link -> entries.put(link.itemId(),rarities.get(link.itemId())));
            specialItems.put(box.id(),Map.copyOf(entries));
        }
        catalogSpecialItems = Map.copyOf(specialItems);
    }
    /** Chance oficial do grupo, sem supor uma distribuição oficial entre suas opções. */
    public Chance specialGroupForCase(WeaponCase box, List<CaseItem> links) {
        if (forCase(box,links).isEmpty()) return null;
        var expected = catalogSpecialItems.get(box.getExternalId());
        if (expected == null || expected.isEmpty()) return null;
        Map<String,String> actual = new HashMap<>();
        for (var link : links) {
            if (link.isSpecial() && actual.put(link.getItem().getExternalId(),link.getItem().getRarity().getId()) != null) return null;
        }
        if (!expected.equals(actual)) return null;
        return new Chance(BigDecimal.valueOf(200).divide(BigDecimal.valueOf(782),8,RoundingMode.HALF_UP),
                "OFFICIAL_RULES_CALCULATED","SPECIAL_GROUP",SOURCE);
    }
    public Map<String,Chance> forCase(WeaponCase box, List<CaseItem> links) {
        if (!"BYMYKEL".equals(box.getSource()) || !"Case".equals(box.getType()) || !revision.equals(box.getSourceRevision())) return Map.of();
        var expected = verifiedCases.get(box.getExternalId());
        if (expected == null) return Map.of();
        Map<String,String> actual = new HashMap<>();
        for (var link : links) {
            if (link.isSpecial()) continue;
            var item = link.getItem();
            if (actual.put(item.getExternalId(),item.getRarity().getId()) != null) return Map.of();
        }
        if (!expected.equals(actual)) return Map.of(); // Não divide por uma lista parcial ou divergente.
        var counts = actual.values().stream().collect(Collectors.groupingBy(value -> value,Collectors.counting()));
        Map<String,Chance> result = new HashMap<>();
        actual.forEach((id,rarity) -> {
            BigDecimal percent = BigDecimal.valueOf(WEIGHTS.get(rarity) * 100L)
                    .divide(BigDecimal.valueOf(782L * counts.get(rarity)),8,RoundingMode.HALF_UP);
            result.put(id,new Chance(percent,"OFFICIAL_RULES_CALCULATED","SKIN_ALL_VARIANTS",SOURCE));
        });
        return Map.copyOf(result);
    }
}
