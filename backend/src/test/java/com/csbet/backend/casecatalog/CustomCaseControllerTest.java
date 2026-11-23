package com.csbet.backend.casecatalog;
import com.csbet.backend.auth.TokenService;
import com.csbet.backend.config.*;
import com.csbet.backend.exception.ApiExceptionHandler;
import java.math.BigDecimal;
import java.util.List;
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
@WebMvcTest(CustomCaseController.class)
@Import({SecurityConfig.class,JwtConfig.class,TokenService.class,CatalogExceptionHandler.class,ApiExceptionHandler.class})
class CustomCaseControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CustomCaseService service;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry){registry.add("JWT_SECRET",()->"custom-catalog-tests-only-secret-longer-than-thirty-two-characters");}
    @Test void catalogAndContentsArePublicWithoutCreatingOpeningOrCredit() throws Exception {
        var box=new CustomCaseDtos.Summary("custom-all-win","All Win","99%/1%","/images/custom-all-win.svg","#00d4ff",new BigDecimal("3.00"),true,2,"V1");
        when(service.list(0,"","price_asc")).thenReturn(new CustomCaseDtos.Page(List.of(box),0,12,1,1));
        when(service.detail("custom-all-win")).thenReturn(new CustomCaseDtos.Detail(box,List.of(),"Fictício"));
        mvc.perform(get("/api/custom-cases?sort=price_asc")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].openingPrice").value("3.00"));
        mvc.perform(get("/api/custom-cases/custom-all-win")).andExpect(status().isOk()).andExpect(jsonPath("$.box.available").value(true));
        mvc.perform(post("/api/openings").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
    }
    @Test void unknownCustomCaseIs404AndInvalidFiltersAre400() throws Exception {
        when(service.detail(anyString())).thenThrow(new CatalogNotFoundException());
        when(service.list(eq(-1),anyString(),anyString())).thenThrow(new InvalidCatalogRequestException());
        mvc.perform(get("/api/custom-cases/missing")).andExpect(status().isNotFound());
        mvc.perform(get("/api/custom-cases?page=-1")).andExpect(status().isBadRequest());
    }
}
