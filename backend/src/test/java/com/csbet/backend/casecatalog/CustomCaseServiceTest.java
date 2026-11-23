package com.csbet.backend.casecatalog;
import com.csbet.backend.market.*;
import com.csbet.backend.skin.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class CustomCaseServiceTest {
    CustomCaseRepository definitions=mock(CustomCaseRepository.class);
    CustomCaseEntryRepository entries=mock(CustomCaseEntryRepository.class);
    RandomGenerator random=mock(RandomGenerator.class);
    CustomCaseService service=new CustomCaseService(definitions,entries,Clock.systemUTC(),random);
    CustomCaseDefinition definition;
    List<CustomCaseEntry> pool;
    @BeforeEach void setup() {
        var box=new WeaponCase("CSBET","custom-all-win");box.updateCustom("All Win","99% básico, 1% faca","/images/custom-all-win.svg","CSBET_CUSTOM_V1");
        ReflectionTestUtils.setField(box,"id",1L);
        definition=new CustomCaseDefinition(box,new BigDecimal("3.00"),"#00d4ff","CSBET_CUSTOM_V1","0".repeat(64));
        ReflectionTestUtils.setField(definition,"caseId",1L);
        pool=List.of(entry(1L,"normal",9900,false),entry(2L,"knife",100,true));
        when(definitions.findByBoxExternalId("custom-all-win")).thenReturn(Optional.of(definition));
        when(entries.findContents(1L)).thenReturn(pool);
    }
    CustomCaseEntry entry(Long id,String external,int weight,boolean special) {
        var skin=new CosmeticItem("BYMYKEL",external);skin.update(external,"Test",special?"knife":"rifle",special?"knife":"none",null,new Rarity("rare","Rare","#4b69ff"));
        ReflectionTestUtils.setField(skin,"id",id);
        var variant=mock(MarketVariant.class);when(variant.getId()).thenReturn(id);when(variant.getSkinId()).thenReturn(id);when(variant.getWear()).thenReturn(special?Wear.NONE:Wear.FT);
        when(variant.getCurrency()).thenReturn("BRL");when(variant.getPriceAmount()).thenReturn(new BigDecimal("2.00"));
        when(variant.getQuoteSource()).thenReturn("STEAM_COMMUNITY_MARKET");when(variant.getProvider()).thenReturn("TEST");
        when(variant.getQuotedAt()).thenReturn(LocalDateTime.now(ZoneOffset.UTC));when(variant.getValidUntil()).thenReturn(LocalDateTime.now(ZoneOffset.UTC).plusHours(1));
        return new CustomCaseEntry(definition,skin,weight,List.of(variant));
    }
    @Test void allTenThousandTicketsProveNinetyNinePercentBasicAndOnePercentKnife() {
        var counts=new HashMap<String,Integer>();
        for(int ticket=0;ticket<10000;ticket++)counts.merge(CustomCaseService.select(pool,ticket).getSkin().getExternalId(),1,Integer::sum);
        assertThat(counts).containsEntry("normal",9900).containsEntry("knife",100);
        when(random.nextInt(10000)).thenReturn(9999);
        assertThat(service.draw(service.prepare("custom-all-win"))).isSameAs(pool.get(1));
    }
    @Test void incompleteWeightsAreUnavailableWithoutDrawingOrNormalizingThem() {
        when(entries.findContents(1L)).thenReturn(List.of(pool.getFirst()));
        assertThatThrownBy(()->service.prepare("custom-all-win")).isInstanceOf(CustomCaseUnavailableException.class);
        assertThat(service.detail("custom-all-win").box().available()).isFalse();verifyNoInteractions(random);
    }
    @Test void absentCaseIsNotFound() {
        assertThatThrownBy(()->service.prepare("custom-missing")).isInstanceOf(CatalogNotFoundException.class);
    }
    @Test void rejectsVariantFromAnotherSkinAndNeverDrawsAgainstPrices() {
        when(pool.getFirst().getVariants().getFirst().getSkinId()).thenReturn(99L);
        assertThatThrownBy(()->service.prepare("custom-all-win")).isInstanceOf(CustomCaseUnavailableException.class);
        verifyNoInteractions(random);
    }
    @Test void manifestHasEighteenUniqueBoxesPublishedVariantsAndExactAllWinOdds() {
        var manifest=CustomCaseManifest.bundled();assertThat(manifest.boxes()).hasSize(18);
        assertThat(manifest.boxes().stream().map(CustomCaseManifest.Box::id)).doesNotHaveDuplicates();
        var allWin=manifest.boxes().stream().filter(b->b.id().equals("custom-all-win")).findFirst().orElseThrow();
        assertThat(allWin.entries()).hasSize(2);assertThat(allWin.entries()).extracting(CustomCaseManifest.Entry::weight).containsExactly(9900,100);
        for(var box:manifest.boxes())assertThat(box.entries().stream().mapToInt(CustomCaseManifest.Entry::weight).sum()).isEqualTo(10000);
    }

    @Test void newCollectionsDrawExactlyTheirPublishedWeightsWithoutIntroducingVariants() {
        var manifest=CustomCaseManifest.bundled();
        var added=Set.of("custom-midnight","custom-toxic-bloom","custom-mecha-core","custom-crimson-relay");
        var existing=manifest.boxes().stream().filter(b->!added.contains(b.id())).flatMap(b->b.entries().stream()).toList();
        for(var box:manifest.boxes().stream().filter(b->added.contains(b.id())).toList()) {
            var candidates=new ArrayList<CustomCaseEntry>();
            for(var item:box.entries()) {
                assertThat(existing.stream().filter(e->e.externalId().equals(item.externalId()))
                        .flatMap(e->e.variants().stream()).toList()).containsAll(item.variants());
                var skin=new CosmeticItem("BYMYKEL",item.externalId());
                candidates.add(new CustomCaseEntry(definition,skin,item.weight(),List.of()));
            }
            var observed=new HashMap<String,Integer>();
            for(int ticket=0;ticket<10000;ticket++)
                observed.merge(CustomCaseService.select(candidates,ticket).getSkin().getExternalId(),1,Integer::sum);
            for(var item:box.entries())assertThat(observed).containsEntry(item.externalId(),item.weight());
        }
    }
}
