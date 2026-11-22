package com.csbet.backend.market;

import com.csbet.backend.config.*;
import com.csbet.backend.exception.ApiExceptionHandler;
import com.csbet.backend.skin.*;
import java.util.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MarketCatalogController.class)
@Import({SecurityConfig.class,JwtConfig.class,PasswordConfig.class,MarketCatalogService.class,MarketExceptionHandler.class,ApiExceptionHandler.class})
class MarketCatalogControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean MarketCatalogRepository repository;
    @MockitoBean MarketQueryService quotes;
    @MockitoBean Clock pricingClock;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET",()->UUID.randomUUID().toString()+UUID.randomUUID());
    }
    @BeforeEach void clock() { when(pricingClock.instant()).thenReturn(Instant.parse("2026-11-22T12:00:00Z")); }
    private CosmeticItem skin() {
        var item=new CosmeticItem("BYMYKEL","skin-fixture");
        item.update("AK-47 | Fixture","AK-47","rifles","none",null,new Rarity("rare","Rare","#4b69ff"));
        ReflectionTestUtils.setField(item,"id",1L); return item;
    }
    @Test void publicCombinedFiltersArePagedAndQuotesAreBatchedAndWearScoped() throws Exception {
        when(repository.skins(anyString(),anyString(),anyString(),any(),anyString(),any(),any()))
                .thenReturn(new PageImpl<>(List.of(skin()),PageRequest.of(1,12),25));
        var fn=new MarketDtos.QuoteView(1L,"AVAILABLE",new java.math.BigDecimal("50.00"),"BRL","MARKET_CSGO","TEST",
                OffsetDateTime.now(),OffsetDateTime.now().plusDays(1),Wear.FN,false,false,"Fixture (Factory New)",null,null,null,null);
        var ft=new MarketDtos.QuoteView(2L,"AVAILABLE",new java.math.BigDecimal("20.00"),"BRL","MARKET_CSGO","TEST",
                OffsetDateTime.now(),OffsetDateTime.now().plusDays(1),Wear.FT,false,false,"Fixture (Field-Tested)",null,null,null,null);
        when(quotes.skinQuotes(List.of(1L))).thenReturn(Map.of(1L,List.of(fn,ft)));
        mvc.perform(get("/api/market/skins").param("page","1").param("size","12").param("weapon","AK-47")
                .param("rarity","rare").param("wear","FN").param("search","Fixture").param("sort","price_desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(25)).andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].id").value("skin-fixture"))
                .andExpect(jsonPath("$.content[0].marketPrices.length()").value(1))
                .andExpect(jsonPath("$.content[0].marketPrices[0].wear").value("FN"))
                .andExpect(jsonPath("$.content[0].simulatedPrice").doesNotExist());
        verify(repository).skins(eq("Fixture"),eq("AK-47"),eq("rare"),eq(Wear.FN),eq("price_desc"),eq(PageRequest.of(1,12)),any());
        verify(quotes,times(1)).skinQuotes(List.of(1L));
    }
    @Test void skinsWithoutQuotesRemainVisibleAndAreNotAssignedAPrice() throws Exception {
        when(repository.skins(anyString(),anyString(),anyString(),isNull(),anyString(),any(),any()))
                .thenReturn(new PageImpl<>(List.of(skin())));
        mvc.perform(get("/api/market/skins")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].marketPrices").isEmpty());
    }
    @Test void invalidFiltersAre400AndNoWriteEndpointIsPublic() throws Exception {
        for (var query:List.of("page=-1","size=61","page=abc","wear=INVALID","sort=featured","page=100001"))
            mvc.perform(get("/api/market/skins?"+query)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/market/skins")).andExpect(status().isUnauthorized());
        verifyNoInteractions(repository,quotes);
    }
}
