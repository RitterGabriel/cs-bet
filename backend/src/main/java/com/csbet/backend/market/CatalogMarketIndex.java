package com.csbet.backend.market;

import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Relaciona somente hashes presentes no feed a IDs do snapshot oficial em inglês.
 * Nomes ambíguos (ex.: fases Doppler com o mesmo nome) ficam sem cotação, nunca são adivinhados. */
final class CatalogMarketIndex {
    record Match(MarketPriceSource.Target target, String externalId, Wear wear, boolean statTrak, boolean souvenir) { }
    private final Map<String, String> skins;
    private final Map<String, String> cases;
    private static final Map<String, Wear> WEARS = Map.of(
            "Factory New", Wear.FN, "Minimal Wear", Wear.MW, "Field-Tested", Wear.FT,
            "Well-Worn", Wear.WW, "Battle-Scarred", Wear.BS);
    CatalogMarketIndex() { this(read("skins"), read("crates")); }
    CatalogMarketIndex(JsonNode skins, JsonNode cases) { this.skins = uniqueNames(skins); this.cases = uniqueNames(cases); }
    private static JsonNode read(String file) {
        try (var input = CatalogMarketIndex.class.getResourceAsStream("/catalog/" + file + ".json")) {
            if (input == null) throw new IllegalStateException("Snapshot do catálogo ausente.");
            return new JsonMapper().readTree(input);
        } catch (java.io.IOException failure) { throw new IllegalStateException("Snapshot do catálogo indisponível.", failure); }
    }
    private static Map<String, String> uniqueNames(JsonNode array) {
        if (!array.isArray()) throw new IllegalArgumentException("Snapshot inválido.");
        Map<String, String> names = new HashMap<>();
        Set<String> ambiguous = new HashSet<>();
        for (var item : array) {
            String name = item.path("name").asText("");
            String id = item.path("id").asText("");
            if (name.isBlank() || id.isBlank()) throw new IllegalArgumentException("Identificação ausente.");
            if (names.putIfAbsent(name, id) != null) ambiguous.add(name);
        }
        ambiguous.forEach(names::remove);
        return Map.copyOf(names);
    }
    Match match(String hash) {
        String caseId = cases.get(hash);
        if (caseId != null) return new Match(MarketPriceSource.Target.CASE, caseId, Wear.NONE, false, false);
        String name = hash;
        Wear wear = Wear.NONE;
        for (var entry : WEARS.entrySet()) {
            String suffix = " (" + entry.getKey() + ")";
            if (name.endsWith(suffix)) { wear = entry.getValue(); name = name.substring(0, name.length() - suffix.length()); break; }
        }
        boolean statTrak = false, souvenir = false;
        if (name.startsWith("★ StatTrak™ ")) { statTrak = true; name = "★ " + name.substring("★ StatTrak™ ".length()); }
        else if (name.startsWith("StatTrak™ ")) { statTrak = true; name = name.substring("StatTrak™ ".length()); }
        else if (name.startsWith("Souvenir ")) { souvenir = true; name = name.substring("Souvenir ".length()); }
        String id = skins.get(name);
        return id == null ? null : new Match(MarketPriceSource.Target.SKIN, id, wear, statTrak, souvenir);
    }
}
