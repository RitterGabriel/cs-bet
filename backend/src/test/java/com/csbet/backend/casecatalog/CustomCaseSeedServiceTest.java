package com.csbet.backend.casecatalog;
import com.csbet.backend.market.*;
import com.csbet.backend.skin.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class CustomCaseSeedServiceTest {
    @Test void importingTwiceDoesNotDuplicateOrOverwritePublishedDefinitions() {
        var cases=mock(WeaponCaseRepository.class);var definitions=mock(CustomCaseRepository.class);var entries=mock(CustomCaseEntryRepository.class);
        var skins=mock(CosmeticItemRepository.class);var variants=mock(MarketVariantRepository.class);
        var full=CustomCaseManifest.bundled();var spec=full.boxes().stream().filter(b->b.id().equals("custom-all-win")).findFirst().orElseThrow();
        var manifest=new CustomCaseManifest(full.version(),List.of(spec));
        var saved=new HashMap<String,CustomCaseDefinition>();
        when(definitions.findByBoxExternalId(anyString())).thenAnswer(c->Optional.ofNullable(saved.get(c.getArgument(0))));
        when(definitions.saveAndFlush(any())).thenAnswer(c->{CustomCaseDefinition d=c.getArgument(0);saved.put(d.getBox().getExternalId(),d);return d;});
        when(cases.saveAndFlush(any())).thenAnswer(c->{WeaponCase b=c.getArgument(0);ReflectionTestUtils.setField(b,"id",1L);return b;});
        long id=0;
        for(var line:spec.entries()) {
            var skin=new CosmeticItem("BYMYKEL",line.externalId());ReflectionTestUtils.setField(skin,"id",++id);
            skin.update(line.name(),"Test","rifle",id==1?"none":"knife",null,new Rarity("test","Test","#4b69ff"));
            when(skins.findBySourceAndExternalId("BYMYKEL",line.externalId())).thenReturn(Optional.of(skin));
            var pool=new ArrayList<MarketVariant>();
            for(var option:line.variants()) {
                var variant=mock(MarketVariant.class);when(variant.getWear()).thenReturn(option.wear());when(variant.isStatTrak()).thenReturn(option.statTrak());pool.add(variant);
            }
            when(variants.findBySkinIdIn(List.of(skin.getId()))).thenReturn(pool);
        }
        var service=new CustomCaseSeedService(cases,definitions,entries,skins,variants);
        assertThat(service.seed(manifest)).isEqualTo(1);assertThat(service.seed(manifest)).isEqualTo(1);
        verify(cases).saveAndFlush(any());verify(definitions).saveAndFlush(any());verify(entries,times(2)).saveAndFlush(any());
        var changed=new CustomCaseManifest.Box(spec.id(),spec.name(),spec.price().add(BigDecimal.ONE),spec.accent(),spec.description(),spec.image(),spec.entries());
        assertThatThrownBy(()->service.seed(new CustomCaseManifest(full.version(),List.of(changed)))).hasMessageContaining("não pode ser sobrescrita");
        verify(definitions,times(1)).saveAndFlush(any());
    }
}
