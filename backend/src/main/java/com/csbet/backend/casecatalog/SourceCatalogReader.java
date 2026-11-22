package com.csbet.backend.casecatalog;

import java.io.InputStream;
import java.net.URI;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static com.csbet.backend.casecatalog.SourceCatalog.*;

/**
 * Fonte: https://github.com/ByMykel/CSGO-API (MIT, licença distribuída com o snapshot).
 * Usa exclusivamente type=Case e contains/contains_rare da própria caixa. Não expande
 * facas/luvas por nome ou por um grupo global. Não infere wear, pattern, phase ou chances.
 * Artes da Valve NÃO são sublicenciadas pelo MIT: apenas URLs, sem baixar/redistribuir imagens.
 */
public class SourceCatalogReader {
    public static final String SPECIAL_NOTICE = "Itens especiais conforme a lista da fonte; a lista não garante todas as variantes de fase, padrão, desgaste ou StatTrak. Sem probabilidades verificadas.";
    private static final Set<String> WEAPON_CATEGORIES = Set.of("csgo_inventory_weapon_category_rifles",
            "csgo_inventory_weapon_category_pistols", "csgo_inventory_weapon_category_smgs",
            "csgo_inventory_weapon_category_heavy", "loadoutslot_equipment");
    private final JsonMapper mapper = new JsonMapper();

    public SourceCatalog read(InputStream cratesInput, InputStream skinsInput, String revision, String language) {
        if (revision == null || !revision.matches("[a-f0-9]{40}") || !Set.of("en", "pt-BR", "pt-PT").contains(language)) {
            throw new SourceDataException("Revisão ou idioma da fonte inválido.");
        }
        try {
            JsonNode crates = mapper.readTree(cratesInput);
            JsonNode skins = mapper.readTree(skinsInput);
            require(crates != null && crates.isArray() && skins != null && skins.isArray(), "A fonte deve conter arrays JSON.");
            Map<String, JsonNode> sourceItems = new HashMap<>();
            for (JsonNode skin : skins) {
                String id = text(skin, "id", 120);
                require(sourceItems.put(id, skin) == null, "ID de item duplicado na fonte.");
            }
            Map<String, SourceItem> selectedItems = new LinkedHashMap<>();
            List<SourceCase> selectedCases = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (JsonNode box : crates) {
                if (!isWeaponCase(box)) continue;
                String id = text(box, "id", 120);
                require(id.startsWith("crate-") && ids.add(id), "ID de caixa inválido ou duplicado.");
                JsonNode regular = box.path("contains");
                require(regular.isArray() && !regular.isEmpty(), "Caixa de armas sem conteúdo regular.");
                JsonNode rare = box.path("contains_rare");
                require(rare.isMissingNode() || rare.isNull() || rare.isArray(), "Grupo especial inválido.");
                List<SourceLink> links = new ArrayList<>();
                Set<String> linked = new HashSet<>();
                addLinks(regular, false, sourceItems, selectedItems, links, linked);
                if (rare.isArray()) addLinks(rare, true, sourceItems, selectedItems, links, linked);
                String notice = (rare.isArray() && !rare.isEmpty() ? "" : "Grupo especial não informado ou vazio. ") + SPECIAL_NOTICE;
                selectedCases.add(new SourceCase(id, text(box, "name", 250), optional(box, "description", 4000),
                        image(box), List.copyOf(links), notice));
            }
            require(!selectedCases.isEmpty(), "Nenhuma caixa oficial de armas encontrada; importação cancelada.");
            return new SourceCatalog(List.copyOf(selectedCases), List.copyOf(selectedItems.values()), revision, language);
        } catch (SourceDataException exception) {
            throw exception;
        } catch (Exception exception) {
            // Não propaga o JSON/HTML da resposta remota nem detalhes internos para logs.
            throw new SourceDataException("Não foi possível ler o JSON do catálogo. Nenhum dado foi importado.");
        }
    }

    public static boolean isWeaponCase(JsonNode node) {
        return "Case".equals(node.path("type").asText(""));
    }

    private void addLinks(JsonNode nodes, boolean special, Map<String, JsonNode> sourceItems,
                          Map<String, SourceItem> selected, List<SourceLink> links, Set<String> linked) {
        for (JsonNode reference : nodes) {
            String id = text(reference, "id", 120);
            require(linked.add(id), "Item repetido dentro da mesma caixa.");
            JsonNode skin = sourceItems.get(id);
            require(skin != null, "Item associado sem detalhes na fonte; importação cancelada.");
            String category = text(skin.path("category"), "id", 120);
            String specialCategory = switch (category) {
                case "sfui_invpanel_filter_melee" -> "knife";
                case "sfui_invpanel_filter_gloves" -> "glove";
                default -> "none";
            };
            require(special ? !specialCategory.equals("none") : WEAPON_CATEGORIES.contains(category),
                    "Categoria incompatível com uma caixa oficial de armas.");
            if (!selected.containsKey(id)) {
                JsonNode rarity = skin.path("rarity");
                String color = text(rarity, "color", 7);
                require(color.matches("#[a-fA-F0-9]{6}"), "Cor de raridade inválida.");
                selected.put(id, new SourceItem(id, text(skin, "name", 250), text(skin.path("weapon"), "name", 120),
                        category, specialCategory, image(skin), new SourceRarity(text(rarity, "id", 80), text(rarity, "name", 100), color)));
            }
            links.add(new SourceLink(id, special));
        }
    }

    private static String text(JsonNode node, String field, int max) {
        String value = optional(node, field, max);
        require(value != null && !value.isBlank(), "Campo obrigatório ausente ou inválido na fonte: " + field);
        return value;
    }
    private static String optional(JsonNode node, String field, int max) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) return null;
        require(value.isString() && value.asText().length() <= max, "Campo inválido na fonte: " + field);
        return value.asText();
    }
    private static String image(JsonNode node) {
        String url = optional(node, "image", 2048);
        if (url == null || url.isBlank()) return null;
        URI uri = URI.create(url);
        require("https".equals(uri.getScheme()) && uri.getUserInfo() == null && Set.of(
                "community.akamai.steamstatic.com", "cdn.steamstatic.com", "raw.githubusercontent.com").contains(uri.getHost()),
                "URL de imagem fora dos hosts públicos permitidos.");
        return url;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new SourceDataException(message);
    }
}
