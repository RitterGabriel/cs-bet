package com.csbet.backend.pricing;

import com.csbet.backend.config.*;
import com.csbet.backend.exception.ApiExceptionHandler;
import com.csbet.backend.skin.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
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

@WebMvcTest(PriceController.class)
@Import({SecurityConfig.class, JwtConfig.class, PasswordConfig.class, PriceQueryService.class, PriceExceptionHandler.class, ApiExceptionHandler.class})
class PriceControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean SimulatedPriceRepository prices;
    @MockitoBean PriceHistoryRepository history;
    @MockitoBean CosmeticItemRepository skins;
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String secret = UUID.randomUUID().toString() + UUID.randomUUID(); registry.add("JWT_SECRET", () -> secret);
    }
    private SimulatedPrice price;
    @BeforeEach
    void setup() {
        var skin = new CosmeticItem("BYMYKEL", "skin-fixture"); ReflectionTestUtils.setField(skin, "id", 1L);
        when(skins.findBySourceAndExternalId("BYMYKEL", "skin-fixture")).thenReturn(Optional.of(skin));
        price = new SimulatedPrice(skin, new BigDecimal("100.00"), LocalDate.of(2026, 11, 21), LocalDateTime.of(2026, 11, 21, 12, 0));
        when(prices.findById(1L)).thenReturn(Optional.of(price));
    }
    @Test
    void priceIsPublicReadOnlyAndClearlySimulated() throws Exception {
        mvc.perform(get("/api/skins/skin-fixture/price")).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPrice").value("100.00"))
                .andExpect(jsonPath("$.basePrice").value("100.00"))
                .andExpect(jsonPath("$.unit").value("SIMULATED_CREDITS"));
        verify(prices, never()).save(any()); verifyNoInteractions(history);
        mvc.perform(post("/api/skins/skin-fixture/price")).andExpect(status().isUnauthorized());
    }
    @Test
    void historyIsPublicPaginatedAndDoesNotGenerateUpdates() throws Exception {
        when(history.findBySkinId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new PriceHistory(price, "INITIAL")), PageRequest.of(0, 30), 1));
        mvc.perform(get("/api/skins/skin-fixture/price/history")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value("100.00"))
                .andExpect(jsonPath("$.content[0].eventType").value("INITIAL"))
                .andExpect(jsonPath("$.totalElements").value(1));
        verify(history, never()).save(any());
    }
    @Test
    void missingSkinOrUninitializedPriceReturns404AndBadPaging400() throws Exception {
        mvc.perform(get("/api/skins/missing/price")).andExpect(status().isNotFound());
        when(prices.findById(1L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/skins/skin-fixture/price")).andExpect(status().isNotFound());
        mvc.perform(get("/api/skins/skin-fixture/price/history?size=0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/skins/skin-fixture/price/history?page=abc")).andExpect(status().isBadRequest());
    }
}
