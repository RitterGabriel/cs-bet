package com.csbet.backend.opening;

import com.csbet.backend.auth.TokenService;
import com.csbet.backend.config.*;
import com.csbet.backend.exception.ApiExceptionHandler;
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

@WebMvcTest(CaseOpeningController.class)
@Import({SecurityConfig.class, JwtConfig.class, TokenService.class, CaseOpeningExceptionHandler.class, ApiExceptionHandler.class})
class CaseOpeningControllerTest {
    @Autowired MockMvc mvc;
    @Autowired TokenService tokens;
    @MockitoBean CaseOpeningService service;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> "opening-tests-only-secret-long-enough-for-hmac-256-not-a-real-secret");
    }
    String body() { return "{\"caseId\":\"crate-7007\",\"expectedPrice\":\"2.71\",\"requestId\":\"" + UUID.randomUUID() + "\",\"userId\":99}"; }
    @Test void requiresAuthenticationAndUsesOnlyJwtIdentity() throws Exception {
        mvc.perform(post("/api/openings").contentType("application/json").content(body())).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
        mvc.perform(post("/api/openings").header("Authorization", "Bearer " + tokens.issue(7L).accessToken())
                .contentType("application/json").content(body())).andExpect(status().isOk());
        verify(service).open(eq(7L), argThat(r -> r.caseId().equals("crate-7007") && r.expectedPrice().toPlainString().equals("2.71")));
    }
    @Test void validatesPriceUuidAndCaseBeforeCallingService() throws Exception {
        for (String invalid : new String[]{"{}", body().replace("2.71", "-1"), body().replace("2.71", "2.711"), body().replace("crate-7007", "")})
            mvc.perform(post("/api/openings").header("Authorization", "Bearer " + tokens.issue(7L).accessToken())
                    .contentType("application/json").content(invalid)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void returnsUsefulConflictWithoutClaimingAnOpeningSucceeded() throws Exception {
        when(service.open(anyLong(), any())).thenThrow(new OpeningUnavailableException("Saldo fictício insuficiente."));
        mvc.perform(post("/api/openings").header("Authorization", "Bearer " + tokens.issue(7L).accessToken())
                .contentType("application/json").content(body())).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Saldo fictício insuficiente."));
    }
}
