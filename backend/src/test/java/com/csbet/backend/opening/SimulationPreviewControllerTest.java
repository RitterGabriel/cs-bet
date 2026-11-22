package com.csbet.backend.opening;

import com.csbet.backend.auth.TokenService;
import com.csbet.backend.casecatalog.*;
import com.csbet.backend.config.*;
import com.csbet.backend.exception.ApiExceptionHandler;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SimulationPreviewController.class)
@Import({SecurityConfig.class, JwtConfig.class, TokenService.class, OfficialCaseOdds.class,
        SimulationPreviewService.class, SpecialRewardResolver.class,
        SimulationPreviewExceptionHandler.class, ApiExceptionHandler.class})
class SimulationPreviewControllerTest {
    @Autowired MockMvc mvc;
    @Autowired TokenService tokens;
    @MockitoBean WeaponCaseRepository cases;
    @MockitoBean CaseItemRepository contents;
    @MockitoBean com.csbet.backend.repository.UserRepository users;
    @MockitoBean com.csbet.backend.wallet.WalletMovementRepository wallet;
    @MockitoBean com.csbet.backend.inventory.InventoryRepository inventory;
    @MockitoBean com.csbet.backend.market.MarketVariantRepository quotes;
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String secret = UUID.randomUUID().toString() + UUID.randomUUID();
        registry.add("JWT_SECRET", () -> secret);
    }
    private String bearer() { return "Bearer " + tokens.issue(1L).accessToken(); }
    private void loadCompleteCase() {
        var pool = SimulationFixture.pool("crate-7007");
        when(cases.findBySourceAndExternalId("BYMYKEL", "crate-7007")).thenReturn(Optional.of(pool.box()));
        when(contents.findContents(1L)).thenReturn(pool.links());
    }
    @AfterEach
    void neverTouchesBalanceInventoryOrQuotes() {
        verifyNoInteractions(users, wallet, inventory, quotes);
    }

    @Test
    void existingSecurityRequiresJwtWithoutChangingAuthentication() throws Exception {
        for (String token : List.of("", "Bearer invalid")) {
            mvc.perform(post("/api/simulations/preview").header("Authorization", token)
                            .contentType("application/json").content("{\"caseId\":\"crate-7007\",\"samples\":1}"))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(cases, contents);
    }

    @Test
    void returnsAggregatedResultsAndExplicitVariantLimitationsWithoutMutatingRepositories() throws Exception {
        loadCompleteCase();
        mvc.perform(post("/api/simulations/preview").header("Authorization", bearer())
                        .contentType("application/json").content("{\"caseId\":\"crate-7007\",\"samples\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.requestedSamples").value(1))
                .andExpect(jsonPath("$.totalSamples").value(1))
                .andExpect(jsonPath("$.rarities.length()").value(5))
                .andExpect(jsonPath("$.rarities[0].theoreticalChance.percent").isString())
                .andExpect(jsonPath("$.rarities[0].theoreticalChance.sourceUrl").value(OfficialCaseOdds.SOURCE))
                .andExpect(jsonPath("$.rarities[4].rarityId").value("SPECIAL_GROUP"))
                .andExpect(jsonPath("$.rarities[4].scope").value("SPECIAL_GROUP"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].count").value(1))
                .andExpect(jsonPath("$.items[0].externalId").isString())
                .andExpect(jsonPath("$.items[0].rarity.id").isString())
                .andExpect(jsonPath("$.items[0].variant.status").value("UNDETERMINED"))
                .andExpect(jsonPath("$.items[0].variant.wear").isEmpty())
                .andExpect(jsonPath("$.items[0].variant.statTrak").isEmpty())
                .andExpect(jsonPath("$.items[0].variant.phase").isEmpty())
                .andExpect(jsonPath("$.limitations").isArray())
                .andExpect(jsonPath("$..balance").doesNotExist());
        verify(cases).findBySourceAndExternalId("BYMYKEL", "crate-7007");
        verify(contents).findContents(1L);
        verifyNoMoreInteractions(cases, contents);
    }

    @Test
    void sampleLimitIsAcceptedAndEveryDrawIsAccountedFor() throws Exception {
        loadCompleteCase();
        String json = mvc.perform(post("/api/simulations/preview").header("Authorization", bearer())
                        .contentType("application/json").content("{\"caseId\":\"crate-7007\",\"samples\":10000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalSamples").value(10000))
                .andReturn().getResponse().getContentAsString();
        var response = new tools.jackson.databind.json.JsonMapper().readTree(json);
        int rarityTotal = 0;
        for (var rarity : response.path("rarities")) rarityTotal += rarity.path("count").asInt();
        int itemTotal = 0;
        for (var item : response.path("items")) itemTotal += item.path("count").asInt();
        org.assertj.core.api.Assertions.assertThat(rarityTotal).isEqualTo(10000);
        org.assertj.core.api.Assertions.assertThat(itemTotal).isEqualTo(10000);
    }

    @Test
    void rejectsInvalidSamplesBeforeReadingCatalog() throws Exception {
        for (String samples : List.of("0", "-1", "10001", "null", "1.5", "1.0", "\"10\"", "true", "2147483648")) {
            mvc.perform(post("/api/simulations/preview").header("Authorization", bearer())
                            .contentType("application/json").content("{\"caseId\":\"crate-7007\",\"samples\":" + samples + "}"))
                    .andExpect(status().isBadRequest());
        }
        for (String body : List.of("{}", "{", "{\"samples\":1}", "{\"caseId\":\"\",\"samples\":1}",
                "{\"caseId\":\"crate-7007\"}", "{\"caseId\":\"" + "a".repeat(121) + "\",\"samples\":1}")) {
            mvc.perform(post("/api/simulations/preview").header("Authorization", bearer())
                            .contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(cases, contents);
    }

    @Test
    void nonexistentCaseReturns404AndIncompleteCaseReturnsUnavailable() throws Exception {
        mvc.perform(post("/api/simulations/preview").header("Authorization", bearer())
                        .contentType("application/json").content("{\"caseId\":\"unknown\",\"samples\":1}"))
                .andExpect(status().isNotFound());
        loadCompleteCase();
        when(contents.findContents(1L)).thenReturn(List.of());
        mvc.perform(post("/api/simulations/preview").header("Authorization", bearer())
                        .contentType("application/json").content("{\"caseId\":\"crate-7007\",\"samples\":10}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.reason").value("UNVERIFIED_OR_INCOMPLETE_CATALOG"))
                .andExpect(jsonPath("$.requestedSamples").value(10))
                .andExpect(jsonPath("$.totalSamples").value(0))
                .andExpect(jsonPath("$.rarities").isEmpty()).andExpect(jsonPath("$.items").isEmpty());
    }
}
