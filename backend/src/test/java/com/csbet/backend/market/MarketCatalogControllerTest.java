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
        when(repository.skins(anyString(),any(),anyString(),anyString(),any(),anyString(),any(),any(),any()))
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
        verify(repository).skins(eq("Fixture"),isNull(),eq("AK-47"),eq("rare"),eq(Wear.FN),eq("price_desc"),eq(PageRequest.of(1,12)),any(),eq(MarketItemKind.ALL));
        verify(quotes,times(1)).skinQuotes(List.of(1L));
    }
    @Test void categoryFiltersWeaponsWhileKeepingOtherFiltersAndServerPaging() throws Exception {
        when(repository.skins(anyString(),any(),anyString(),anyString(),any(),anyString(),any(),any(),any()))
                .thenReturn(new PageImpl<>(List.of(skin()),PageRequest.of(0,24),50));
        when(repository.weaponOptions(any())).thenReturn(List.of(
                new MarketCatalogDtos.WeaponOption("AK-47","csgo_inventory_weapon_category_rifles","none"),
                new MarketCatalogDtos.WeaponOption("AWP","csgo_inventory_weapon_category_rifles","none"),
                new MarketCatalogDtos.WeaponOption("Glock-18","csgo_inventory_weapon_category_pistols","none"),
                new MarketCatalogDtos.WeaponOption("Karambit","sfui_invpanel_filter_melee","knife"),
                new MarketCatalogDtos.WeaponOption("Hand Wraps","sfui_invpanel_filter_gloves","glove"),
                new MarketCatalogDtos.WeaponOption("Zeus x27","loadoutslot_equipment","none")));
        mvc.perform(get("/api/market/skins").param("category","rifles").param("weapon","AK-47")
                .param("rarity","rare").param("wear","FT").param("sort","price_asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.weapons").value(org.hamcrest.Matchers.contains("AK-47","AWP")))
                .andExpect(jsonPath("$.categories[?(@.id == 'knives')].weapons[0]").value(org.hamcrest.Matchers.contains("Karambit")))
                .andExpect(jsonPath("$.categories[?(@.id == 'gloves')].weapons[0]").value(org.hamcrest.Matchers.contains("Hand Wraps")))
                .andExpect(jsonPath("$.categories[?(@.id == 'equipment')].weapons[0]").value(org.hamcrest.Matchers.contains("Zeus x27")));
        verify(repository).skins(eq(""),eq(MarketCategory.RIFLES),eq("AK-47"),eq("rare"),eq(Wear.FT),eq("price_asc"),eq(PageRequest.of(0,24)),any(),eq(MarketItemKind.ALL));
    }
    @Test void incompatibleCategoryAndWeaponYieldEmptyResultsAndAllRestoresWeaponOptions() throws Exception {
        when(repository.skins(anyString(),any(),anyString(),anyString(),any(),anyString(),any(),any(),any()))
                .thenReturn(Page.empty(PageRequest.of(0,24)));
        when(repository.weaponOptions(any())).thenReturn(List.of(
                new MarketCatalogDtos.WeaponOption("AK-47","csgo_inventory_weapon_category_rifles","none"),
                new MarketCatalogDtos.WeaponOption("Glock-18","csgo_inventory_weapon_category_pistols","none")));
        mvc.perform(get("/api/market/skins").param("category","rifles").param("weapon","Glock-18"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        verify(repository).skins(eq(""),eq(MarketCategory.RIFLES),eq("Glock-18"),eq(""),isNull(),eq("name"),any(),any(),eq(MarketItemKind.ALL));
        mvc.perform(get("/api/market/skins").param("category","all"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.weapons").value(org.hamcrest.Matchers.contains("AK-47","Glock-18")));
    }
    @Test void skinsWithoutQuotesRemainVisibleAndAreNotAssignedAPrice() throws Exception {
        when(repository.skins(anyString(),any(),anyString(),anyString(),isNull(),anyString(),any(),any(),any()))
                .thenReturn(new PageImpl<>(List.of(skin())));
        mvc.perform(get("/api/market/skins")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].marketPrices").isEmpty());
    }
    @Test void agentCategoryHasServerPaginationAndCannotAcceptWeaponWear() throws Exception {
        var agent=new CosmeticItem("BYMYKEL","agent-4613");
        agent.update("Agent Fixture","Terrorist","agent","none",null,new Rarity("rarity_legendary_character","Superior","#d32ce6"));
        ReflectionTestUtils.setField(agent,"id",10L);
        when(repository.skins(anyString(),any(),anyString(),anyString(),any(),anyString(),any(),any(),eq(MarketItemKind.ALL)))
                .thenReturn(new PageImpl<>(List.of(agent),PageRequest.of(1,24),63));
        when(repository.weaponOptions(MarketItemKind.ALL)).thenReturn(List.of(new MarketCatalogDtos.WeaponOption("Terrorist","agent","none")));
        mvc.perform(get("/api/market/skins").param("category","agents").param("page","1").param("sort","price_asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(63)).andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].id").value("agent-4613"))
                .andExpect(jsonPath("$.content[0].weaponType").value("agent"))
                .andExpect(jsonPath("$.categories[0].id").value("agents"));
        verify(repository).skins(eq(""),eq(MarketCategory.AGENTS),eq(""),eq(""),isNull(),eq("price_asc"),eq(PageRequest.of(1,24)),any(),eq(MarketItemKind.ALL));
        mvc.perform(get("/api/market/skins").param("category","agents").param("wear","FT")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/market/skins").param("kind","invalid")).andExpect(status().isBadRequest());
    }
    @Test void allCategoriesIncludeAgentsAndSkinsInTheSameServerPage() throws Exception {
        var agent = new CosmeticItem("BYMYKEL", "agent-4613");
        agent.update("Agent Fixture", "Terrorist", "agent", "none", null,
                new Rarity("rarity_legendary_character", "Superior", "#d32ce6"));
        ReflectionTestUtils.setField(agent, "id", 10L);
        when(repository.skins(anyString(), isNull(), anyString(), anyString(), isNull(), anyString(), any(), any(), eq(MarketItemKind.ALL)))
                .thenReturn(new PageImpl<>(List.of(skin(), agent)));
        mvc.perform(get("/api/market/skins").param("category", "all"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value("skin-fixture"))
                .andExpect(jsonPath("$.content[1].id").value("agent-4613"));
    }
    @Test void invalidFiltersAre400AndNoWriteEndpointIsPublic() throws Exception {
        for (var query:List.of("page=-1","size=61","page=abc","wear=INVALID","sort=featured","page=100001","category=invented","category=rifles OR 1=1"))
            mvc.perform(get("/api/market/skins?"+query)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/market/skins")).andExpect(status().isUnauthorized());
        verifyNoInteractions(repository,quotes);
    }
}
