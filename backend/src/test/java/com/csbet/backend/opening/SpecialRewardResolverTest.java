package com.csbet.backend.opening;

import com.csbet.backend.casecatalog.*;
import com.csbet.backend.skin.*;
import java.util.*;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpecialRewardResolverTest {
    private final SourceCatalog catalog = new CatalogSourceLoader().bundled();
    private final OfficialCaseOdds odds = new OfficialCaseOdds();
    private final WeaponCaseRepository cases = mock(WeaponCaseRepository.class);
    private final CaseItemRepository contents = mock(CaseItemRepository.class);
    private final RandomGenerator random = mock(RandomGenerator.class);
    private final SpecialRewardResolver resolver = new SpecialRewardResolver(cases,contents,odds,random);
    private record Pool(WeaponCase box, List<CaseItem> links) { }
    private Pool pool(String id, long databaseId) {
        var source = catalog.cases().stream().filter(c->c.id().equals(id)).findFirst().orElseThrow();
        var items = catalog.items().stream().collect(java.util.stream.Collectors.toMap(SourceCatalog.SourceItem::id,s->s));
        var box = new WeaponCase("BYMYKEL",id);
        box.update(source.name(),null,null,catalog.revision(),catalog.language(),source.specialNotice());
        ReflectionTestUtils.setField(box,"id",databaseId);
        List<CaseItem> links = new ArrayList<>();
        for (var link : source.links()) {
            var data = items.get(link.itemId()); var skin = new CosmeticItem("BYMYKEL",data.id());
            skin.update(data.name(),data.weapon(),data.weaponType(),data.specialCategory(),data.imageUrl(),new Rarity(data.rarity().id(),data.rarity().name(),data.rarity().color()));
            ReflectionTestUtils.setField(skin,"id",databaseId*1000+links.size()+1);
            links.add(new CaseItem(box,skin,link.special()));
        }
        when(cases.findBySourceAndExternalId("BYMYKEL",id)).thenReturn(Optional.of(box));
        when(contents.findContents(databaseId)).thenReturn(links);
        return new Pool(box,links);
    }
    @Test void everyCatalogSpecialIdIsSelectedByExactlyOneUniformIndexIncludingDopplerPhases() {
        var pool = pool("crate-7007",1);
        var expected = pool.links().stream().filter(CaseItem::isSpecial).map(l->l.getItem().getExternalId()).collect(java.util.stream.Collectors.toSet());
        assertThat(expected).hasSize(52);
        Set<String> selected = new HashSet<>();
        for (int index=0;index<expected.size();index++) {
            when(random.nextInt(expected.size())).thenReturn(index);
            var result = resolver.resolveSpecialWin("crate-7007");
            assertThat(result.skinId()).isNotNull(); selected.add(result.externalId());
        }
        assertThat(selected).containsExactlyInAnyOrderElementsOf(expected);
        verify(random,times(52)).nextInt(52);
    }
    @Test void differentCasesUseOnlyTheirOwnSpecialPoolAndReturnTheFinalSkinId() {
        var knives = pool("crate-7007",1); var gloves = pool("crate-4288",2);
        when(random.nextInt(52)).thenReturn(0); when(random.nextInt(24)).thenReturn(23);
        var knife = resolver.resolveSpecialWin("crate-7007"); var glove = resolver.resolveSpecialWin("crate-4288");
        assertThat(knives.links().stream().filter(CaseItem::isSpecial).map(l->l.getItem().getId())).contains(knife.skinId());
        assertThat(gloves.links().stream().filter(CaseItem::isSpecial).map(l->l.getItem().getId())).contains(glove.skinId());
        assertThat(knife.skinId()).isNotEqualTo(glove.skinId());
        verify(random).nextInt(52); verify(random).nextInt(24);
    }
    @Test void missingOrIncompleteCasesCannotTriggerADraw() {
        assertThatThrownBy(()->resolver.resolveSpecialWin("unknown")).isInstanceOf(CatalogNotFoundException.class);
        var pool = pool("crate-7007",1); var partial = new ArrayList<>(pool.links()); partial.removeLast();
        when(contents.findContents(1L)).thenReturn(partial);
        assertThatThrownBy(()->resolver.resolveSpecialWin("crate-7007")).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(random);
    }
    @Test void invalidDatabaseIdentifiersAreRejectedBeforeAnyRandomSelection() {
        var pool = pool("crate-7007",1);
        ReflectionTestUtils.setField(pool.links().getLast().getItem(),"id",null);
        assertThatThrownBy(()->resolver.resolveSpecialWin("crate-7007")).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(random);
    }
}
