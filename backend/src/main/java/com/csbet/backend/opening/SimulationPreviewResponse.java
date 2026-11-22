package com.csbet.backend.opening;

import com.csbet.backend.casecatalog.OfficialCaseOdds;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.List;

/** Frequências e probabilidades são percentuais de 0 a 100, serializados como decimais. */
public record SimulationPreviewResponse(
        String status, String reason, String caseId, int requestedSamples, int totalSamples,
        List<RarityCount> rarities, List<ItemCount> items, List<String> limitations) {
    public record RarityCount(String rarityId, String scope, int count,
                             @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal observedPercent,
                             OfficialCaseOdds.Chance theoreticalChance) { }
    public record Rarity(String id, String name, String color) { }
    public record Variant(String status, String wear, Boolean statTrak, String phase, String reason) { }
    public record ItemCount(Long skinId, String externalId, String name, Rarity rarity,
                            boolean special, int count,
                            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal observedPercent,
                            String selectionBasis, boolean officialItemProbabilityVerified,
                            Variant variant) { }
}
