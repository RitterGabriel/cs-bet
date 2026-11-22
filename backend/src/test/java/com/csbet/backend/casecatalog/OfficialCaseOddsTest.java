package com.csbet.backend.casecatalog;

import com.csbet.backend.skin.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;

class OfficialCaseOddsTest {
    private final SourceCatalog catalog = new CatalogSourceLoader().bundled();
    private final OfficialCaseOdds odds = new OfficialCaseOdds(catalog);
    private WeaponCase box(String id) {
        var box = new WeaponCase("BYMYKEL",id);
        box.update("Fixture",null,null,catalog.revision(),catalog.language(),"Fixture");
        ReflectionTestUtils.setField(box,"id",1L);
        return box;
    }
    List<CaseItem> links(WeaponCase box) {
        var entries = catalog.items().stream().collect(java.util.stream.Collectors.toMap(SourceCatalog.SourceItem::id,x->x));
        var source = catalog.cases().stream().filter(x->x.id().equals(box.getExternalId())).findFirst().orElseThrow();
        List<CaseItem> result = new ArrayList<>();
        for (var link : source.links()) {
            var data = entries.get(link.itemId());
            var item = new CosmeticItem("BYMYKEL",data.id());
            item.update(data.name(),data.weapon(),data.weaponType(),data.specialCategory(),data.imageUrl(),
                    new Rarity(data.rarity().id(),data.rarity().name(),data.rarity().color()));
            ReflectionTestUtils.setField(item,"id",(long)result.size()+1);
            result.add(new CaseItem(box,item,link.special()));
        }
        return result;
    }
    @Test void feverUsesOfficialEqualWithinRarityRuleAndKeepsPhaseSubdivisionsUnverified() {
        var box = box("crate-7007"); var links = links(box); var chances = odds.forCase(box,links);
        assertThat(chances).hasSize(17);
        assertThat(chances.get("skin-4c2e3face5fd").percent()).isEqualByComparingTo("1.06564365"); // AK-47 | Searing Rage
        assertThat(chances.get("skin-5a39103af835").percent()).isEqualByComparingTo("0.31969309"); // AWP | Printstream
        for (var link : links) {
            if (link.isSpecial()) assertThat(chances).doesNotContainKey(link.getItem().getExternalId());
            else {
                var chance = chances.get(link.getItem().getExternalId());
                assertThat(chance.sourceUrl()).isEqualTo(OfficialCaseOdds.SOURCE);
                assertThat(chance.scope()).isEqualTo("SKIN_ALL_VARIANTS");
                assertThat(chance.basis()).isEqualTo("OFFICIAL_RULES_CALCULATED");
            }
        }
    }
    @Test void itemDtoExposesVerifiedChanceAsADecimalStringWithSourceAndScope() {
        var box = box("crate-7007"); var all = links(box); var link = all.stream().filter(item->!item.isSpecial()).findFirst().orElseThrow();
        var dto = CatalogDtos.ItemResponse.from(link,null,List.of(),odds.forCase(box,all).get(link.getItem().getExternalId()));
        tools.jackson.databind.JsonNode node = new tools.jackson.databind.json.JsonMapper().valueToTree(dto);
        assertThat(node.path("dropChance").path("percent").isString()).isTrue();
        assertThat(node.path("dropChance").path("sourceUrl").asText()).isEqualTo(OfficialCaseOdds.SOURCE);
        assertThat(node.path("dropChance").path("scope").asText()).isEqualTo("SKIN_ALL_VARIANTS");
    }
    @Test void verifiedSpecialPoolsExposeOnlyTheGroupChanceInsteadOfIndividualOfficialRates() {
        for (var id : List.of("crate-4288","crate-4471","crate-4880","crate-4017","crate-4001")) {
            var box = box(id); var all = links(box); var chances = odds.forCase(box,all);
            var group = odds.specialGroupForCase(box,all);
            assertThat(group.percent()).isEqualByComparingTo("0.25575448");
            assertThat(group.scope()).isEqualTo("SPECIAL_GROUP");
            for (var link : all) if (link.isSpecial()) assertThat(chances).doesNotContainKey(link.getItem().getExternalId());
            var total = chances.values().stream().map(OfficialCaseOdds.Chance::percent).reduce(group.percent(),java.math.BigDecimal::add);
            assertThat(total.doubleValue()).isCloseTo(100.0,within(0.000001));
        }
    }
    @Test void partialDuplicatedOrChangedSpecialPoolsDoNotChangeTheRegularOdds() {
        var box = box("crate-4288"); var all = links(box);
        var incomplete = new ArrayList<>(all); incomplete.removeIf(link -> link.isSpecial() && link.getItem().getExternalId().equals(all.getLast().getItem().getExternalId()));
        var result = odds.forCase(box,incomplete);
        assertThat(result).hasSize(17);
        assertThat(odds.specialGroupForCase(box,incomplete)).isNull();
        assertThat(incomplete.stream().filter(CaseItem::isSpecial)).allSatisfy(link -> assertThat(result).doesNotContainKey(link.getItem().getExternalId()));
        var duplicate = new ArrayList<>(all); duplicate.add(all.getLast());
        assertThat(odds.forCase(box,duplicate)).hasSize(17);
        assertThat(odds.specialGroupForCase(box,duplicate)).isNull();
        var item = all.getLast().getItem();
        item.update(item.getName(),item.getWeapon(),item.getWeaponType(),item.getSpecialCategory(),item.getImageUrl(),new Rarity("unknown","Unknown","#ffffff"));
        assertThat(odds.forCase(box,all)).hasSize(17);
    }
    @Test void phasePoolsRemainUnverifiedEvenWhenTheViewCollapsesDuplicateNames() {
        for (String id : List.of("crate-7007","crate-4061","crate-4236")) {
            var box = box(id); var all = links(box); var names = new HashSet<String>();
            var collapsed = all.stream().filter(link -> !link.isSpecial() || names.add(link.getItem().getName())).toList();
            assertThat(collapsed.size()).isLessThan(all.size());
            assertThat(odds.specialGroupForCase(box,all)).isNotNull();
            assertThat(odds.specialGroupForCase(box,collapsed)).isNull();
            var chances = odds.forCase(box,collapsed);
            assertThat(chances.size()).isEqualTo(collapsed.stream().filter(link->!link.isSpecial()).count());
            assertThat(collapsed.stream().filter(CaseItem::isSpecial)).allSatisfy(link -> assertThat(chances).doesNotContainKey(link.getItem().getExternalId()));
        }
    }
    @Test void allFortyTwoVerifiedCasesHaveOneSpecialGroupIncludingPhasePools() {
        int verified = 0;
        for (var source : catalog.cases()) {
            var box = box(source.id()); var all = links(box);
            var group = odds.specialGroupForCase(box,all);
            assertThat(group).isNotNull();
            assertThat(group.percent()).isEqualByComparingTo("0.25575448");
            verified++;
        }
        assertThat(verified).isEqualTo(42);
    }
    @Test void incompleteDuplicatedOrDifferentRarityListsHaveNoVerifiedPercentages() {
        var box = box("crate-7007"); var all = links(box);
        var missing = new ArrayList<>(all); missing.removeFirst(); assertThat(odds.forCase(box,missing)).isEmpty();
        var duplicate = new ArrayList<>(all); duplicate.add(all.getFirst()); assertThat(odds.forCase(box,duplicate)).isEmpty();
        var item = all.getFirst().getItem();
        item.update(item.getName(),item.getWeapon(),item.getWeaponType(),item.getSpecialCategory(),item.getImageUrl(),new Rarity("unknown","Unknown","#ffffff"));
        assertThat(odds.forCase(box,all)).isEmpty();
    }
    @Test void unverifiedRevisionOrCustomSourcesAreNeverLabeledOfficial() {
        var box = box("crate-7007"); var all = links(box);
        ReflectionTestUtils.setField(box,"sourceRevision","0".repeat(40));assertThat(odds.forCase(box,all)).isEmpty();
        ReflectionTestUtils.setField(box,"sourceRevision",catalog.revision());
        ReflectionTestUtils.setField(box,"source","CUSTOM");assertThat(odds.forCase(box,all)).isEmpty();
    }
    @Test void everyVerifiedStandardCaseHasAllRegularItemsWithConsistentTotalProbability() {
        for (var source : catalog.cases()) {
            var box = box(source.id()); var all = links(box); var chances = odds.forCase(box,all);
            var regular = all.stream().filter(link->!link.isSpecial()).map(link->chances.get(link.getItem().getExternalId())).toList();
            assertThat(regular).doesNotContainNull();
            var sum = regular.stream().map(OfficialCaseOdds.Chance::percent).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add);
            assertThat(sum.doubleValue()).isCloseTo(100.0-200.0/782.0,within(0.000001));
        }
    }
}
