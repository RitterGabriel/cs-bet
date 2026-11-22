package com.csbet.backend.market;

import java.util.Arrays;

/** IDs estruturados de weaponType e specialCategory importados do catálogo ByMykel. */
public enum MarketCategory {
    RIFLES("rifles", "Rifles", "csgo_inventory_weapon_category_rifles", "none"),
    PISTOLS("pistols", "Pistolas", "csgo_inventory_weapon_category_pistols", "none"),
    SMGS("smgs", "Submetralhadoras", "csgo_inventory_weapon_category_smgs", "none"),
    HEAVY("heavy", "Armas pesadas", "csgo_inventory_weapon_category_heavy", "none"),
    KNIVES("knives", "Facas", "sfui_invpanel_filter_melee", "knife"),
    GLOVES("gloves", "Luvas", "sfui_invpanel_filter_gloves", "glove"),
    EQUIPMENT("equipment", "Equipamentos", "loadoutslot_equipment", "none"),
    AGENTS("agents", "Agentes", "agent", "none");

    private final String id;
    private final String label;
    private final String weaponType;
    private final String specialCategory;

    MarketCategory(String id, String label, String weaponType, String specialCategory) {
        this.id = id;
        this.label = label;
        this.weaponType = weaponType;
        this.specialCategory = specialCategory;
    }
    public String id() { return id; }
    public String label() { return label; }

    public static MarketCategory fromFilter(String value) {
        if (value.isEmpty() || value.equals("all")) return null;
        return Arrays.stream(values()).filter(category -> category.id.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Categoria inválida."));
    }
    public static MarketCategory classify(String weaponType, String specialCategory) {
        // A identificação explícita de facas e luvas tem prioridade sobre categorias de armas.
        if ("knife".equals(specialCategory)) return KNIVES;
        if ("glove".equals(specialCategory)) return GLOVES;
        if (!"none".equals(specialCategory)) return null;
        return Arrays.stream(values()).filter(category -> category.specialCategory.equals("none")
                && category.weaponType.equals(weaponType)).findFirst().orElse(null);
    }
    String predicate() {
        String special = "s.specialCategory='" + specialCategory + "'";
        return specialCategory.equals("none") ? special + " and s.weaponType='" + weaponType + "'" : special;
    }
}
