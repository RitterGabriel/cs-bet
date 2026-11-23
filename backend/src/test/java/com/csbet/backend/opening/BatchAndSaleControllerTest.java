package com.csbet.backend.opening;
import com.csbet.backend.auth.TokenService;
import com.csbet.backend.config.*;
import com.csbet.backend.exception.ApiExceptionHandler;
import com.csbet.backend.inventory.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({OpeningBatchController.class,InventorySaleController.class,BulkInventorySaleController.class})
@Import({SecurityConfig.class,JwtConfig.class,TokenService.class,CaseOpeningExceptionHandler.class,InventorySaleExceptionHandler.class,ApiExceptionHandler.class})
class BatchAndSaleControllerTest {
    @Autowired MockMvc mvc;
    @Autowired TokenService tokens;
    @MockitoBean OpeningBatchService batches;
    @MockitoBean InventorySaleService sales;
    @MockitoBean BulkInventorySaleService bulk;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> "batch-sale-tests-only-secret-over-thirty-two-characters-not-real");
    }
    String body(int quantity) { return "{\"caseId\":\"case\",\"quantity\":"+quantity+",\"expectedPrice\":\"2.71\",\"requestId\":\""+UUID.randomUUID()+"\",\"userId\":99}"; }
    String bearer() { return "Bearer "+tokens.issue(7L).accessToken(); }
    @Test void bothEndpointsRequireJwtAndIgnoreFrontendIdentity() throws Exception {
        for(String url:new String[]{"/api/openings/batch","/api/inventory/12/sell"})
            mvc.perform(post(url).contentType("application/json").content(body(2))).andExpect(status().isUnauthorized());
        verifyNoInteractions(batches,sales);
        mvc.perform(post("/api/openings/batch").header("Authorization",bearer()).contentType("application/json").content(body(2))).andExpect(status().isOk());
        verify(batches).open(eq(7L),argThat(r -> r.quantity()==2));
        mvc.perform(post("/api/inventory/12/sell").header("Authorization",bearer()).contentType("application/json").content(body(2))).andExpect(status().isOk());
        verify(sales).sell(eq(7L),eq(12L),any());
    }
    @Test void refusesUnsupportedBatchCountBeforeService() throws Exception {
        for(int count:new int[]{0,3,11}) mvc.perform(post("/api/openings/batch").header("Authorization",bearer()).contentType("application/json").content(body(count)))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(batches);
    }
    @Test void foreignOrMissingInventoryUnitHasSameNotFoundResponse() throws Exception {
        when(sales.sell(anyLong(),anyLong(),any())).thenThrow(new com.csbet.backend.casecatalog.CatalogNotFoundException());
        mvc.perform(post("/api/inventory/99/sell").header("Authorization",bearer()).contentType("application/json").content(body(2)))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Item não encontrado."));
    }
    @Test void bulkSaleRequiresJwtUsesOwnerAndValidatesUniqueUnits() throws Exception {
        mvc.perform(get("/api/inventory/sell-all/quote")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/inventory/sell-all").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        verifyNoInteractions(bulk);
        mvc.perform(get("/api/inventory/sell-all/quote?userId=99").header("Authorization",bearer())).andExpect(status().isOk());
        verify(bulk).quote(7L);
        String line="{\"inventoryId\":12,\"expectedPrice\":\"2.71\"}";
        String start="{\"requestId\":\""+UUID.randomUUID()+"\",\"items\":[";
        mvc.perform(post("/api/inventory/sell-all").header("Authorization",bearer()).contentType("application/json").content(start+line+"]}"))
            .andExpect(status().isOk());verify(bulk).sell(eq(7L),argThat(r->r.items().getFirst().inventoryId()==12L));
        mvc.perform(post("/api/inventory/sell-all").header("Authorization",bearer()).contentType("application/json").content(start+line+","+line+"]}"))
            .andExpect(status().isBadRequest());
    }

}
