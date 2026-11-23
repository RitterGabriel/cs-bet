package com.csbet.backend.opening;

import com.csbet.backend.market.*;
import java.util.*;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VariantSelectorTest {
    static MarketVariant variant(long id, Wear wear, boolean statTrak) {
        var variant = mock(MarketVariant.class);
        when(variant.getId()).thenReturn(id); when(variant.getSkinId()).thenReturn(1L);
        when(variant.getWear()).thenReturn(wear); when(variant.isStatTrak()).thenReturn(statTrak);
        return variant;
    }
    @Test void drawsWearThenFlagAndNeverUsesPriceOrFreshnessAsWeight() {
        var random = mock(RandomGenerator.class);
        var selector = new VariantSelector(random);
        var fn = variant(1, Wear.FN, false); var ft = variant(2, Wear.FT, false); var st = variant(3, Wear.FT, true);
        when(random.nextInt(2)).thenReturn(1, 1);
        assertThat(selector.select(List.of(st, fn, ft), 1L, false)).isSameAs(st);
        verify(random, times(2)).nextInt(2);
        verify(st, never()).getPriceAmount(); verify(st, never()).getValidUntil();
    }
    @Test void restrictsToSkinExcludesSouvenirAndCannotGiveStatTrakGloves() {
        var random = mock(RandomGenerator.class); when(random.nextInt(anyInt())).thenReturn(0);
        var normal = variant(1, Wear.FT, false); var st = variant(2, Wear.FT, true);
        var souvenir = variant(3, Wear.FT, false); when(souvenir.isSouvenir()).thenReturn(true);
        var other = variant(4, Wear.FN, false); when(other.getSkinId()).thenReturn(2L);
        assertThat(new VariantSelector(random).select(List.of(st, souvenir, other, normal), 1L, true)).isSameAs(normal);
    }
    @Test void noKnownVariantsIsExplicitlyUnavailableRatherThanInvented() {
        assertThatThrownBy(() -> new VariantSelector().select(List.of(), 1L, false)).isInstanceOf(OpeningUnavailableException.class);
    }
}
