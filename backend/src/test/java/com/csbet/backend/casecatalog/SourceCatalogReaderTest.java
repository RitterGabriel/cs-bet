package com.csbet.backend.casecatalog;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class SourceCatalogReaderTest {
    private final SourceCatalogReader reader = new SourceCatalogReader();
    private static final String REVISION = CatalogSourceLoader.REVISION;
    // Fixtures mínimas de teste: não são seed nem dados oficiais cadastrados.
    private static final String SKINS = """
            [{"id":"skin-test","name":"Fixture item","weapon":{"name":"Fixture weapon"},
            "category":{"id":"csgo_inventory_weapon_category_rifles"},
            "rarity":{"id":"rarity_rare_weapon","name":"Mil-Spec Grade","color":"#4b69ff"}}]
            """;
    private static final String BOX = """
            [{"id":"crate-test","name":"Fixture case","type":"Case","contains":[{"id":"skin-test"}]}]
            """;
    private SourceCatalog read(String boxes, String skins) {
        return reader.read(new ByteArrayInputStream(boxes.getBytes(StandardCharsets.UTF_8)),
                new ByteArrayInputStream(skins.getBytes(StandardCharsets.UTF_8)), REVISION, "en");
    }
    @Test
    void verifiesFullPinnedSnapshotAndCounts() {
        var source = new CatalogSourceLoader().bundled();
        assertThat(source.cases()).hasSize(42);
        assertThat(source.items()).hasSize(1305);
        assertThat(source.cases().stream().mapToInt(box -> box.links().size()).sum()).isEqualTo(2508);
        assertThat(source.revision()).isEqualTo(REVISION);
        assertThat(source.items()).allSatisfy(item -> {
            assertThat(item.rarity().color()).matches("#[0-9a-fA-F]{6}");
            assertThat(item.weapon()).isNotBlank();
        });
    }
    @Test
    void identifiesOnlyWeaponCases() {
        var mapper = new JsonMapper();
        for (String type : List.of("Souvenir", "Sticker Capsule", "Music Kit Box", "Autograph Capsule", "Graffiti")) {
            assertThat(SourceCatalogReader.isWeaponCase(mapper.readTree("{\"type\":\"" + type + "\"}"))).isFalse();
        }
        assertThat(SourceCatalogReader.isWeaponCase(mapper.readTree("{\"type\":\"Case\"}"))).isTrue();
        assertThatThrownBy(() -> read(BOX.replace("Case\"", "Souvenir\""), SKINS)).isInstanceOf(SourceDataException.class);
    }
    @Test
    void distinguishesCaseSpecificGlovesFromKnives() {
        var source = new CatalogSourceLoader().bundled();
        var revolution = source.cases().stream().filter(box -> box.id().equals("crate-4880")).findFirst().orElseThrow();
        var kilowatt = source.cases().stream().filter(box -> box.id().equals("crate-4904")).findFirst().orElseThrow();
        var items = source.items().stream().collect(java.util.stream.Collectors.toMap(SourceCatalog.SourceItem::id, item -> item));
        assertThat(revolution.links().stream().filter(SourceCatalog.SourceLink::special).toList()).hasSize(24)
                .allSatisfy(link -> assertThat(items.get(link.itemId()).specialCategory()).isEqualTo("glove"));
        assertThat(kilowatt.links().stream().filter(SourceCatalog.SourceLink::special).toList()).hasSize(13)
                .allSatisfy(link -> assertThat(items.get(link.itemId()).specialCategory()).isEqualTo("knife"));
        assertThat(revolution.links().stream().filter(SourceCatalog.SourceLink::special).map(SourceCatalog.SourceLink::itemId).toList())
                .doesNotContainAnyElementsOf(kilowatt.links().stream().filter(SourceCatalog.SourceLink::special).map(SourceCatalog.SourceLink::itemId).toList());
    }
    @Test
    void missingSpecialListIsExplicitlyIncompleteAndMissingImageIsAllowed() {
        var source = read(BOX, SKINS);
        assertThat(source.cases().getFirst().specialNotice()).contains("não informado").contains("não garante");
        assertThat(source.cases().getFirst().imageUrl()).isNull();
        assertThat(source.items().getFirst().imageUrl()).isNull();
    }
    @Test
    void rejectsBrokenOrIncompleteDataBeforePersistence() {
        for (String boxes : List.of("{}", "{", BOX.replace("skin-test", "skin-missing"),
                BOX.replace("[{\"id\":\"skin-test\"}]", "[]"), BOX.replace("Fixture case", ""))) {
            assertThatThrownBy(() -> read(boxes, SKINS)).isInstanceOf(SourceDataException.class);
        }
        assertThatThrownBy(() -> read(BOX, SKINS.replace("#4b69ff", "invalid"))).isInstanceOf(SourceDataException.class);
        assertThatThrownBy(() -> read(BOX, SKINS.replace("Fixture weapon", ""))).isInstanceOf(SourceDataException.class);
    }
    @Test
    void rejectsDuplicateAssociation() {
        String repeated = BOX.replace("[{\"id\":\"skin-test\"}]", "[{\"id\":\"skin-test\"},{\"id\":\"skin-test\"}]");
        assertThatThrownBy(() -> read(repeated, SKINS)).isInstanceOf(SourceDataException.class);
    }
    @Test
    void malformedSourceOptionsAndUnreadableFilesFailClearly() {
        var loader = new CatalogSourceLoader();
        assertThatThrownBy(() -> loader.remote("main", "en")).isInstanceOf(SourceDataException.class);
        assertThatThrownBy(() -> loader.local(java.nio.file.Path.of("/missing/crates.json"), java.nio.file.Path.of("/missing/skins.json"), REVISION, "en"))
                .isInstanceOf(SourceDataException.class);
    }
}
