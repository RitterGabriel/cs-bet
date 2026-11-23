package com.csbet.backend.opening;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import com.csbet.backend.market.Wear;

public record OpeningResponse(Long openingId, Long inventoryId, String caseId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal charged,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal balance,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal referencePrice,
        Item item, Variant variant) {
    public record Item(Long skinId, String externalId, String name, String imageUrl,
                       SimulationPreviewResponse.Rarity rarity, boolean special) { }
    public record Variant(Wear wear, boolean statTrak, String phase, String rule) { }
    static OpeningResponse from(CaseOpening opening) {
        var unit = opening.getInventory(); var skin = unit.getSkin(); var rarity = skin.getRarity();
        return new OpeningResponse(opening.getId(), unit.getId(), opening.getBox().getExternalId(),
                opening.getCharged().setScale(2), opening.getResultingBalance().setScale(2), opening.getReferencePrice() == null ? null : opening.getReferencePrice().setScale(2),
                new Item(skin.getId(), skin.getExternalId(), unit.getDisplayName(), skin.getImageUrl(),
                        new SimulationPreviewResponse.Rarity(rarity.getId(), rarity.getName(), rarity.getColor()), opening.isSpecial()),
                new Variant(unit.getWear(), Boolean.TRUE.equals(unit.getStatTrak()), unit.getPhase(), VariantSelector.RULE));
    }
}
