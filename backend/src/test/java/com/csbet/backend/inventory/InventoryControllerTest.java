package com.csbet.backend.inventory;

import com.csbet.backend.auth.TokenService;
import com.csbet.backend.config.*;
import com.csbet.backend.entity.User;
import com.csbet.backend.exception.ApiExceptionHandler;
import com.csbet.backend.skin.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@Import({SecurityConfig.class, JwtConfig.class, PasswordConfig.class, TokenService.class,
        com.csbet.backend.pricing.PricingConfig.class, InventoryService.class, InventoryExceptionHandler.class, ApiExceptionHandler.class})
class InventoryControllerTest {
    @Autowired MockMvc mvc;
    @Autowired TokenService tokens;
    @MockitoBean InventoryRepository repository;
    @MockitoBean com.csbet.backend.market.MarketQueryService market;
    @MockitoBean com.csbet.backend.pricing.PriceQueryService prices;
    private final Map<Long, List<InventoryItem>> owned = new HashMap<>();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String secret = UUID.randomUUID().toString() + UUID.randomUUID();
        registry.add("JWT_SECRET", () -> secret);
    }
    @BeforeEach
    void setUp() {
        var skin = new CosmeticItem("BYMYKEL", "skin-fixture");
        skin.update("Fixture skin", "Fixture weapon", "rifles", "none", null,
                new Rarity("rarity_rare_weapon", "Mil-Spec Grade", "#4b69ff"));
        ReflectionTestUtils.setField(skin, "id", 50L);
        owned.put(1L, List.of(item(101L, 1L, skin), item(102L, 1L, skin)));
        owned.put(2L, List.of(item(201L, 2L, skin)));
        when(repository.estimateAvailable(anyLong(), eq(InventoryStatus.AVAILABLE), any(java.time.LocalDateTime.class))).thenReturn(
                new InventoryValuation(BigDecimal.ZERO, 0L, 0L));
        when(repository.findByOwnerId(anyLong(), any(Pageable.class))).thenAnswer(call ->
                page(owned.getOrDefault(call.getArgument(0), List.of()), call.getArgument(1)));
        when(repository.findByOwnerIdAndStatus(anyLong(), any(), any(Pageable.class))).thenAnswer(call -> {
            List<InventoryItem> result = owned.getOrDefault(call.getArgument(0), List.of()).stream()
                    .filter(item -> item.getStatus() == call.getArgument(1)).toList();
            return page(result, call.getArgument(2));
        });
    }
    private InventoryItem item(Long id, Long ownerId, CosmeticItem skin) {
        User owner = new User("fixture", "fixture@example.com", "private-hash", BigDecimal.ZERO, LocalDateTime.now());
        ReflectionTestUtils.setField(owner, "id", ownerId);
        var item = new InventoryItem(owner, skin);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
    private Page<InventoryItem> page(List<InventoryItem> items, Pageable pageable) {
        int from = Math.min(items.size(), (int) pageable.getOffset());
        int to = Math.min(items.size(), from + pageable.getPageSize());
        return new PageImpl<>(items.subList(from, to), pageable, items.size());
    }
    private String bearer(Long id) { return "Bearer " + tokens.issue(id).accessToken(); }

    @Test
    void missingOrInvalidJwtReturns401() throws Exception {
        mvc.perform(get("/api/inventory/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/inventory/me").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }
    @Test
    void readsOnlyJwtOwnerAndCannotOverrideOwnerWithQueryParameter() throws Exception {
        mvc.perform(get("/api/inventory/me").param("userId", "2").header("Authorization", bearer(1L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(101))
                .andExpect(jsonPath("$.content[1].id").value(102))
                .andExpect(jsonPath("$.content[0].skinId").value(50))
                .andExpect(jsonPath("$.content[1].skinId").value(50))
                .andExpect(jsonPath("$.content[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.content[0].obtainedAt").isString())
                .andExpect(jsonPath("$.content[0].rarity.color").value("#4b69ff"))
                .andExpect(jsonPath("$..owner").doesNotExist())
                .andExpect(jsonPath("$..passwordHash").doesNotExist())
                .andExpect(jsonPath("$..email").doesNotExist())
                .andExpect(jsonPath("$.content[0].salePrice").isEmpty());
        verify(repository).findByOwnerId(eq(1L), any(Pageable.class));
        mvc.perform(get("/api/inventory/me").header("Authorization", bearer(2L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(201));
    }
    @Test
    void returnsEmptyInventoryForOwnerWithoutItems() throws Exception {
        mvc.perform(get("/api/inventory/me").header("Authorization", bearer(3L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test
    void paginatesIndividualUnitsWithStableOrdering() throws Exception {
        mvc.perform(get("/api/inventory/me").param("page", "1").param("size", "1").header("Authorization", bearer(1L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(102))
                .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.totalPages").value(2));
        var paging = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByOwnerId(eq(1L), paging.capture());
        assertThat(paging.getValue().getSort().getOrderFor("obtainedAt").isDescending()).isTrue();
        assertThat(paging.getValue().getSort().getOrderFor("id").isDescending()).isTrue();
    }
    @Test
    void supportsStatusFilter() throws Exception {
        mvc.perform(get("/api/inventory/me").param("status", "SOLD").header("Authorization", bearer(1L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        verify(repository).findByOwnerIdAndStatus(eq(1L), eq(InventoryStatus.SOLD), any(Pageable.class));
        verify(repository, never()).findByOwnerId(anyLong(), any());
    }
    @Test
    void rejectsInvalidPageAndStatusWithoutQueryingRepository() throws Exception {
        for (String query : List.of("page=-1", "page=abc", "size=0", "size=61", "page=100001", "status=OTHER")) {
            mvc.perform(get("/api/inventory/me?" + query).header("Authorization", bearer(1L)))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(repository);
    }
    @Test
    void startsAvailableWithNoSaleAndMapsNoFinancialFields() {
        var item = owned.get(1L).getFirst();
        assertThat(item.getStatus()).isEqualTo(InventoryStatus.AVAILABLE);
        assertThat(item.getSoldAt()).isNull(); assertThat(item.getSalePrice()).isNull();
        var response = InventoryResponse.from(item);
        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.externalId()).isEqualTo("skin-fixture");
        assertThat(response.obtainedAt().getOffset()).isEqualTo(java.time.ZoneOffset.UTC);
    }
    @Test
    void priceSortIsValidatedAndPassedToOwnedPaginatedQuery() throws Exception {
        when(repository.findOwnedByPrice(anyLong(),nullable(InventoryStatus.class),any(),anyString(),any(Pageable.class)))
            .thenAnswer(call -> page(owned.getOrDefault(call.getArgument(0),List.of()),call.getArgument(4)));
        for(String direction:List.of("PRICE_ASC","PRICE_DESC")) {
            mvc.perform(get("/api/inventory/me").param("sort",direction).param("status","AVAILABLE").param("page","1").param("size","1")
                    .param("userId","2").header("Authorization",bearer(1L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(102));
            verify(repository).findOwnedByPrice(eq(1L),eq(InventoryStatus.AVAILABLE),any(),eq(direction),argThat(p->p.getPageNumber()==1&&p.getPageSize()==1));
        }
        mvc.perform(get("/api/inventory/me").param("sort","UNKNOWN").header("Authorization",bearer(1L))).andExpect(status().isBadRequest());
    }

}
