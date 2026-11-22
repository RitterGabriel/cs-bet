package com.csbet.backend.market;

import com.csbet.backend.config.*;
import com.csbet.backend.exception.ApiExceptionHandler;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MarketController.class)
@Import({SecurityConfig.class, JwtConfig.class, PasswordConfig.class, MarketExceptionHandler.class, ApiExceptionHandler.class})
class MarketControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean MarketQueryService service;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        String secret = UUID.randomUUID().toString() + UUID.randomUUID(); registry.add("JWT_SECRET", () -> secret);
    }
    @Test
    void publicQuotesAreUnavailableWithoutTrustedDataAndHaveNoWriteRoute() throws Exception {
        when(service.forCase("crate-fixture")).thenReturn(MarketDtos.QuoteView.unavailable());
        when(service.forSkin("skin-fixture")).thenReturn(List.of());
        mvc.perform(get("/api/cases/crate-fixture/market-price")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNAVAILABLE")).andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.currentPrice").isEmpty());
        mvc.perform(get("/api/skins/skin-fixture/market-prices")).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(post("/api/skins/skin-fixture/market-prices")).andExpect(status().isUnauthorized());
    }
    @Test
    void historyExplicitlyIdentifiesMarketOriginAndInvalidIdsReturn404() throws Exception {
        when(service.history(1L, 0, 30)).thenReturn(new MarketDtos.HistoryPage(List.of(), 0, 30, 0, 0, "MARKET_REFERENCE"));
        mvc.perform(get("/api/market/variants/1/history")).andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("MARKET_REFERENCE"));
        when(service.forSkin("missing")).thenThrow(new MarketNotFoundException());
        mvc.perform(get("/api/skins/missing/market-prices")).andExpect(status().isNotFound());
    }
}
