package com.csbet.backend.market;

import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class MarketCategoryTest {
    @Test
    void allSnapshotItemsAreClassifiedByStructuredCategoryRatherThanSkinName() throws Exception {
        var counts = new EnumMap<MarketCategory,Integer>(MarketCategory.class);
        try (var input = getClass().getResourceAsStream("/catalog/skins.json")) {
            var items = new JsonMapper().readTree(input);
            for (var item : items) {
                String type = item.path("category").path("id").asText();
                String special = type.equals("sfui_invpanel_filter_melee") ? "knife"
                        : type.equals("sfui_invpanel_filter_gloves") ? "glove" : "none";
                var category = MarketCategory.classify(type,special);
                assertThat(category).as(item.path("id").asText()).isNotNull();
                counts.merge(category,1,Integer::sum);
            }
            assertThat(counts.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(items.size());
            var skinCategories = EnumSet.allOf(MarketCategory.class);
            skinCategories.remove(MarketCategory.AGENTS);
            assertThat(counts.keySet()).isEqualTo(skinCategories);
        }
    }
    @Test
    void agentsUseTheirStructuredTypeAndAreAvailableAsACategory() {
        assertThat(MarketCategory.classify("agent", "none")).isEqualTo(MarketCategory.AGENTS);
        assertThat(MarketCategory.fromFilter("agents")).isEqualTo(MarketCategory.AGENTS);
        for (var agent : com.csbet.backend.skin.AgentCatalogSource.bundled()) {
            assertThat(agent.id()).startsWith("agent-");
            assertThat(agent.teamId()).isIn("terrorists", "counter-terrorists");
        }
    }
    @Test
    void knivesGlovesAndEquipmentDoNotBecomeRiflesBecauseOfNamesOrConflictingTypes() {
        assertThat(MarketCategory.classify("csgo_inventory_weapon_category_rifles","knife")).isEqualTo(MarketCategory.KNIVES);
        assertThat(MarketCategory.classify("csgo_inventory_weapon_category_rifles","glove")).isEqualTo(MarketCategory.GLOVES);
        assertThat(MarketCategory.classify("loadoutslot_equipment","none")).isEqualTo(MarketCategory.EQUIPMENT);
        assertThat(MarketCategory.classify("unknown","none")).isNull();
        assertThat(MarketCategory.fromFilter("")).isNull();
        assertThat(MarketCategory.fromFilter("all")).isNull();
        assertThatThrownBy(()->MarketCategory.fromFilter("invented")).isInstanceOf(IllegalArgumentException.class);
    }
}
