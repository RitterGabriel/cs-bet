package com.csbet.backend.wallet;

import com.csbet.backend.auth.TokenService;
import com.csbet.backend.config.*;
import com.csbet.backend.entity.User;
import com.csbet.backend.repository.UserRepository;
import com.csbet.backend.exception.ApiExceptionHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@Import({SecurityConfig.class, JwtConfig.class, TokenService.class, WalletService.class,
        WalletExceptionHandler.class, ApiExceptionHandler.class})
class WalletControllerTest {
    @Autowired MockMvc mvc;
    @Autowired TokenService tokens;
    @MockitoBean UserRepository users;
    @MockitoBean WalletMovementRepository movements;
    User owner;
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String secret = UUID.randomUUID().toString() + UUID.randomUUID();
        registry.add("JWT_SECRET", () -> secret);
    }
    @BeforeEach
    void setup() {
        owner = new User("owner", "owner@example.com", "private", new BigDecimal("50.00"), LocalDateTime.now());
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(owner));
        when(movements.findByUserIdAndRequestId(anyLong(), anyString())).thenReturn(Optional.empty());
        when(movements.findByUserId(anyLong(), any(Pageable.class))).thenAnswer(call ->
                new PageImpl<>(List.of(new WalletMovement(owner, new BigDecimal("10.00"), UUID.randomUUID().toString())), call.getArgument(1), 1));
    }
    String bearer(long id) { return "Bearer " + tokens.issue(id).accessToken(); }
    @Test
    void requiresAuthenticationForEveryEndpoint() throws Exception {
        mvc.perform(get("/api/wallet")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/wallet/movements")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/wallet/credits").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/wallet").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        verifyNoInteractions(users, movements);
    }
    @Test
    void readsBalanceOnlyFromJwtIdentity() throws Exception {
        mvc.perform(get("/api/wallet?userId=2").header("Authorization", bearer(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value("50.00"));
        verify(users).findById(1L); verify(users, never()).findById(2L);
        mvc.perform(get("/api/wallet").header("Authorization", bearer(2))).andExpect(status().isUnauthorized());
    }
    @Test
    void addsUpToLimitAndIgnoresBodyIdentity() throws Exception {
        mvc.perform(post("/api/wallet/credits").header("Authorization", bearer(1)).contentType("application/json")
                .content("{\"userId\":2,\"amount\":10000.00,\"requestId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value("10050.00"));
        verify(users).findByIdForUpdate(1L);
        verify(users, never()).findByIdForUpdate(2L);
        verify(movements).save(argThat(movement -> movement.getAmount().compareTo(new BigDecimal("10000")) == 0
                && movement.getResultingBalance().compareTo(new BigDecimal("10050")) == 0));
    }
    @Test
    void rejectsInvalidAmountsWithoutChangingBalance() throws Exception {
        for (String value : List.of("0", "-1", "10000.01", "1.001", "null", "\"abc\"", "10000000000000000000")) {
            mvc.perform(post("/api/wallet/credits").header("Authorization", bearer(1)).contentType("application/json")
                    .content("{\"amount\":" + value + ",\"requestId\":\"" + UUID.randomUUID() + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/wallet/credits").header("Authorization", bearer(1)).contentType("application/json")
                .content("{\"amount\":10}" )).andExpect(status().isBadRequest());
        verifyNoInteractions(users, movements);
    }
    @Test
    void repeatedRequestDoesNotAddTwiceAndRejectsDifferentAmount() throws Exception {
        UUID id = UUID.randomUUID();
        when(movements.findByUserIdAndRequestId(1L, id.toString())).thenReturn(Optional.of(
                new WalletMovement(owner, new BigDecimal("10.00"), id.toString())));
        String body = "{\"amount\":10,\"requestId\":\"" + id + "\"}";
        mvc.perform(post("/api/wallet/credits").header("Authorization", bearer(1)).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value("50.00"));
        mvc.perform(post("/api/wallet/credits").header("Authorization", bearer(1)).contentType("application/json")
                .content(body.replace("amount\":10", "amount\":20"))).andExpect(status().isBadRequest());
        verify(movements, never()).save(any());
    }
    @Test
    void historyUsesOwnerPaginationAndDoesNotExposeUser() throws Exception {
        mvc.perform(get("/api/wallet/movements?userId=2").header("Authorization", bearer(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].type").value("CREDIT_ADDITION"))
                .andExpect(jsonPath("$.content[0].amount").value("10.00"))
                .andExpect(jsonPath("$.content[0].createdAt").isString())
                .andExpect(jsonPath("$..user").doesNotExist()).andExpect(jsonPath("$..passwordHash").doesNotExist());
        verify(movements).findByUserId(eq(1L), argThat(page -> page.getPageSize() == 20
                && page.getSort().getOrderFor("id").isDescending()));
        for (String page : List.of("-1", "100001", "abc"))
            mvc.perform(get("/api/wallet/movements?page=" + page).header("Authorization", bearer(1)))
                    .andExpect(status().isBadRequest());
    }
    @Test
    void supportsExactDecimalStringFromFrontend() throws Exception {
        mvc.perform(post("/api/wallet/credits").header("Authorization", bearer(1)).contentType("application/json")
                .content("{\"amount\":\"25.50\",\"requestId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value("75.50"));
    }
    @Test
    void separatesHistoryAndBalanceOfTwoExistingUsers() throws Exception {
        User other = new User("other", "other@example.com", "private", new BigDecimal("7.00"), LocalDateTime.now());
        when(users.findById(2L)).thenReturn(Optional.of(other));
        when(movements.findByUserId(eq(2L), any(Pageable.class))).thenReturn(Page.empty());
        mvc.perform(get("/api/wallet?userId=1").header("Authorization", bearer(2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value("7.00"));
        mvc.perform(get("/api/wallet/movements?userId=1").header("Authorization", bearer(2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        verify(movements).findByUserId(eq(2L), any(Pageable.class));
        verify(movements, never()).findByUserId(eq(1L), any(Pageable.class));
    }
    @Test
    void entityPreventsNegativeAndOverflowBalances() {
        assertThatThrownBy(() -> owner.addCredits(new BigDecimal("-51"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> owner.addCredits(new BigDecimal("9999999999999999.99"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(owner.getBalance()).isEqualByComparingTo("50.00");
    }
}
