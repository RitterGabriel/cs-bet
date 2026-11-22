package com.csbet.backend.casecatalog;

import com.csbet.backend.config.*;
import com.csbet.backend.exception.ApiExceptionHandler;
import com.csbet.backend.skin.*;
import jakarta.persistence.criteria.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
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

@WebMvcTest(CaseController.class)
@Import({SecurityConfig.class, JwtConfig.class, PasswordConfig.class, CaseCatalogService.class,
        CatalogExceptionHandler.class, ApiExceptionHandler.class})
class CaseControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean com.csbet.backend.market.MarketCatalogRepository marketCatalog;
    @MockitoBean java.time.Clock pricingClock;
    @MockitoBean WeaponCaseRepository cases;
    @MockitoBean CaseItemRepository contents;
    @MockitoBean com.csbet.backend.market.MarketQueryService market;
    @MockitoBean com.csbet.backend.pricing.PriceQueryService prices;
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String secret = UUID.randomUUID().toString() + UUID.randomUUID();
        registry.add("JWT_SECRET", () -> secret);
    }
    private WeaponCase box() {
        var box = new WeaponCase(CatalogImportService.SOURCE, "crate-4880");
        box.update("Revolution Case", null, null, CatalogSourceLoader.REVISION, "en", SourceCatalogReader.SPECIAL_NOTICE);
        ReflectionTestUtils.setField(box, "id", 1L);
        return box;
    }
    @Test
    @SuppressWarnings("unchecked")
    void listIsPublicAndAppliesSearchCategoryAndPagination() throws Exception {
        when(cases.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(box()), PageRequest.of(1, 12), 25));
        mvc.perform(get("/api/cases").param("page", "1").param("size", "12").param("search", "REVOL%_").param("sort", "name"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3)).andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.content[0].id").value("crate-4880"))
                .andExpect(jsonPath("$.content[0].category").value("official"));
        ArgumentCaptor<Specification<WeaponCase>> specification = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> paging = ArgumentCaptor.forClass(Pageable.class);
        verify(cases).findAll(specification.capture(), paging.capture());
        assertThat(paging.getValue().getPageNumber()).isEqualTo(1);
        assertThat(paging.getValue().getPageSize()).isEqualTo(12);
        assertThat(paging.getValue().getSort().getOrderFor("name")).isNotNull();
        Root<WeaponCase> root = mock(Root.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        specification.getValue().toPredicate(root, null, cb);
        verify(root).get("name"); verify(root).get("category");
        verify(cb).equal((Expression<?>) null, "official");
        verify(cb).like((Expression<String>) null, "%revol!%!_%", '!');
    }
    @Test
    void priceSortUsesServerPagingAndNameDescendingIsAccepted() throws Exception {
        when(pricingClock.instant()).thenReturn(java.time.Instant.parse("2026-11-22T12:00:00Z"));
        when(marketCatalog.cases(anyString(),anyString(),anyString(),any(),any())).thenReturn(new PageImpl<>(List.of(box())));
        for (String sort:List.of("price_asc","price_desc")) {
            mvc.perform(get("/api/cases").param("sort",sort)).andExpect(status().isOk());
            verify(marketCatalog).cases(eq(""),eq("official"),eq(sort),eq(PageRequest.of(0,12)),any());
        }
        when(cases.findAll(any(Specification.class),any(Pageable.class))).thenReturn(new PageImpl<>(List.of(box())));
        mvc.perform(get("/api/cases").param("sort","name_desc")).andExpect(status().isOk());
        verify(cases).findAll(any(Specification.class),argThat((Pageable p)->p.getSort().getOrderFor("name").isDescending()));
    }
    @Test
    void detailIsPublicAndSeparatesOnlyItsOwnSpecialItems() throws Exception {
        WeaponCase box = box();
        var normal = new CosmeticItem(CatalogImportService.SOURCE, "skin-normal-test");
        normal.update("Fixture regular", "Fixture weapon", "rifles", "none", null,
                new Rarity("rarity_rare_weapon", "Mil-Spec Grade", "#4b69ff"));
        var special = new CosmeticItem(CatalogImportService.SOURCE, "skin-special-test");
        special.update("Fixture gloves", "Fixture gloves", "gloves", "glove", null,
                new Rarity("rarity_ancient", "Extraordinary", "#eb4b4b"));
        ReflectionTestUtils.setField(normal, "id", 2L); ReflectionTestUtils.setField(special, "id", 3L);
        when(cases.findBySourceAndExternalId(CatalogImportService.SOURCE, "crate-4880")).thenReturn(Optional.of(box));
        when(contents.findContents(1L)).thenReturn(List.of(new CaseItem(box, normal, false), new CaseItem(box, special, true)));
        var current = com.csbet.backend.pricing.PriceDtos.CurrentPrice.from(new com.csbet.backend.pricing.SimulatedPrice(
                normal, new java.math.BigDecimal("100.00"), java.time.LocalDate.now(), java.time.LocalDateTime.now()));
        when(prices.currentForIds(any())).thenReturn(Map.of(2L, current));
        mvc.perform(get("/api/cases/crate-4880")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].simulatedPrice.currentPrice").value("100.00"))
                .andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.specialItems.length()").value(1))
                .andExpect(jsonPath("$.specialItems[0].specialCategory").value("glove"))
                .andExpect(jsonPath("$.specialItems[0].rarity.id").value("rarity_ancient"))
                .andExpect(jsonPath("$.specialNotice").isNotEmpty())
                .andExpect(jsonPath("$.box.passwordHash").doesNotExist());
        verify(contents).findContents(1L);
        verify(prices).currentForIds(List.of(2L, 3L));
    }
    @Test
    void invalidFiltersAndPaginationReturn400() throws Exception {
        for (String query : List.of("page=-1", "page=abc", "page=100001", "size=0", "size=61", "category=capsule", "sort=invalid")) {
            mvc.perform(get("/api/cases?" + query)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/cases").param("search", "x".repeat(151))).andExpect(status().isBadRequest());
        verifyNoInteractions(cases, contents);
    }
    @Test
    void missingCaseReturns404AndImportHasNoPublicRoute() throws Exception {
        mvc.perform(get("/api/cases/missing")).andExpect(status().isNotFound());
        mvc.perform(post("/api/cases/import")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        verifyNoInteractions(contents);
    }
}
