package com.csbet.backend.market;

import com.csbet.backend.skin.CosmeticItem;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MarketCatalogRepositoryTest {
    @Test
    @SuppressWarnings("unchecked")
    void pagingAndCombinedFiltersAreAppliedBeforeFetchingOnlyTheRequestedPage() {
        var manager=mock(EntityManager.class);
        TypedQuery<Object[]> query=mock(TypedQuery.class,RETURNS_SELF);
        TypedQuery<Long> count=mock(TypedQuery.class,RETURNS_SELF);
        when(manager.createQuery(anyString(),eq(Object[].class))).thenReturn(query);
        when(manager.createQuery(anyString(),eq(Long.class))).thenReturn(count);
        var item=new CosmeticItem("BYMYKEL","skin-one");
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{item,new java.math.BigDecimal("10.00")}));
        when(count.getSingleResult()).thenReturn(80L);
        var now=LocalDateTime.of(2026,11,22,12,0);
        var result=new MarketCatalogRepository(manager).skins("AK%_",MarketCategory.RIFLES,"AK-47","rare",Wear.FT,"price_desc",PageRequest.of(2,24),now);
        assertThat(result.getContent()).containsExactly(item);assertThat(result.getTotalElements()).isEqualTo(80);
        verify(manager).createQuery(contains("s.specialCategory='none' and s.weaponType='csgo_inventory_weapon_category_rifles'"),eq(Object[].class));
        verify(manager).createQuery(contains("s.specialCategory='none' and s.weaponType='csgo_inventory_weapon_category_rifles'"),eq(Long.class));
        verify(query).setFirstResult(48);verify(query).setMaxResults(24);verify(query).setParameter("now",now);
        for (var q:List.of(query,count)) {
            verify(q).setParameter("search","%ak!%!_%");verify(q).setParameter("weapon","AK-47");
            verify(q).setParameter("rarity","rare");verify(q).setParameter("wear",Wear.FT);
        }
    }
    @Test
    void priceOrderingUsesMinimumCompatibleFreshQuoteAndAlwaysPlacesMissingQuotesLast() {
        for (String sort:List.of("price_asc","price_desc")) {
            String direction=sort.equals("price_asc")?"asc":"desc";
            String skins=MarketCatalogRepository.skinQuery(sort,false,false,true);
            assertThat(skins).contains("select min(v.priceAmount)","v.validUntil>:now","v.wear=:wear",
                    "is null then 1 else 0 end, minimumPrice "+direction);
            assertThat(MarketCatalogRepository.caseQuery(sort)).contains("v.validUntil>:now",
                    "is null then 1 else 0 end, v.priceAmount "+direction);
        }
    }
}
