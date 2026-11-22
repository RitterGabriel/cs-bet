package com.csbet.backend.casecatalog;

import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Metadados de apresentação do snapshot fixado; não altera identidades ou chances. */
final class SpecialItemMetadata {
    private static final Map<String,String> PHASES = loadPhases();
    private SpecialItemMetadata() { }
    static String phase(WeaponCase box, CaseItem link) {
        return link.isSpecial() && "BYMYKEL".equals(box.getSource()) && CatalogSourceLoader.REVISION.equals(box.getSourceRevision())
                ? PHASES.get(link.getItem().getExternalId()) : null;
    }
    private static Map<String,String> loadPhases() {
        try (var input = SpecialItemMetadata.class.getResourceAsStream("/catalog/crates.json")) {
            if(input==null) return Map.of();
            var cases = new JsonMapper().readTree(input); Map<String,String> phases = new HashMap<>(); Set<String> ambiguous = new HashSet<>();
            for (var box : cases) for (var item : box.path("contains_rare")) {
                var phase = item.path("phase"); if(!phase.isString() || phase.asText().isBlank())continue;
                String id=item.path("id").asText(); String previous=phases.putIfAbsent(id,phase.asText());
                if(previous!=null && !previous.equals(phase.asText()))ambiguous.add(id);
            }
            ambiguous.forEach(phases::remove);return Map.copyOf(phases);
        } catch (Exception failure) { return Map.of(); } // Falha de apresentação nunca inventa uma fase nem impede o catálogo.
    }
}
