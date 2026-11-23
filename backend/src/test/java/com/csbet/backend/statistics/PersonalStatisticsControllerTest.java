package com.csbet.backend.statistics;

import com.csbet.backend.auth.TokenService;
import com.csbet.backend.config.*;
import com.csbet.backend.entity.User;
import com.csbet.backend.exception.ApiExceptionHandler;
import com.csbet.backend.inventory.*;
import com.csbet.backend.repository.UserRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.csbet.backend.statistics.PersonalStatisticsResponse.*;

@WebMvcTest(PersonalStatisticsController.class)
@Import({SecurityConfig.class, JwtConfig.class, TokenService.class, PersonalStatisticsService.class,
        StatisticsExceptionHandler.class, ApiExceptionHandler.class})
class PersonalStatisticsControllerTest {
    @Autowired MockMvc mvc;
    @Autowired TokenService tokens;
    @MockitoBean UserRepository users;
    @MockitoBean StatisticsRepository statistics;
    @MockitoBean InventoryRepository inventory;
    @MockitoBean Clock pricingClock;
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String secret = UUID.randomUUID().toString() + UUID.randomUUID();
        registry.add("JWT_SECRET", () -> secret);
    }
    @BeforeEach
    void setup() {
        when(pricingClock.instant()).thenReturn(Instant.parse("2026-11-22T12:00:00Z"));
        when(users.findById(1L)).thenReturn(Optional.of(new User("owner", "owner@example.invalid", "private", new BigDecimal("42.00"), LocalDateTime.now())));
        when(users.findById(2L)).thenReturn(Optional.of(new User("other", "other@example.invalid", "private", new BigDecimal("8.00"), LocalDateTime.now())));
        when(statistics.walletTotals(anyLong())).thenReturn(new StatisticsRepository.WalletTotals(1, new BigDecimal("100.00")));
        when(statistics.salesTotals(anyLong())).thenReturn(new StatisticsRepository.SalesTotals(1, new BigDecimal("15.00"), 0));
        when(inventory.estimateAvailable(anyLong(), eq(InventoryStatus.AVAILABLE), any()))
                .thenReturn(new InventoryValuation(new BigDecimal("20.00"), 3L, 0L));
        when(statistics.activityCount(anyLong(), any())).thenReturn(1L);
        when(statistics.activity(eq(1L), any(), anyInt(), anyInt())).thenReturn(List.of(new Activity("CREDIT_ADDITION:1", ActivityType.CREDIT_ADDITION,
                OffsetDateTime.parse("2026-11-22T12:00:00Z"), null, new BigDecimal("100.00"), new BigDecimal("100.00"))));
        when(statistics.activity(eq(2L), any(), anyInt(), anyInt())).thenReturn(List.of(new Activity("ITEM_SOLD:9", ActivityType.ITEM_SOLD,
                null, "Other player's item", null, null)));
    }
    String bearer(long id) { return "Bearer " + tokens.issue(id).accessToken(); }
    @Test
    void anonymousInvalidAndDeletedUserSessionsCannotReadStatistics() throws Exception {
        mvc.perform(get("/api/statistics/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/statistics/me").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/statistics/me").header("Authorization", bearer(99))).andExpect(status().isUnauthorized());
        verifyNoInteractions(statistics, inventory);
    }
    @Test
    void onlyJwtOwnerIsUsedAndOpeningTotalsComeFromPersistedDebits() throws Exception {
        mvc.perform(get("/api/statistics/me?userId=2").header("Authorization", bearer(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.summary.balance").value("42.00"))
                .andExpect(jsonPath("$.summary.creditsAdded").value("100.00"))
                .andExpect(jsonPath("$.summary.availableItems").value(3))
                .andExpect(jsonPath("$.summary.inventoryEstimatedValue").value("20.00"))
                .andExpect(jsonPath("$.summary.soldItems").value(1))
                .andExpect(jsonPath("$.summary.salesTotal").value("15.00"))
                .andExpect(jsonPath("$.summary.openingHistoryAvailable").value(true))
                .andExpect(jsonPath("$.summary.openedCases").value(0))
                .andExpect(jsonPath("$.summary.openingSpend").value("0.00"))
                .andExpect(jsonPath("$.activity.content[0].id").value("CREDIT_ADDITION:1"))
                .andExpect(jsonPath("$..passwordHash").doesNotExist()).andExpect(jsonPath("$..email").doesNotExist());
        verify(statistics).walletTotals(1L);
        verify(statistics).salesTotals(1L);
        verify(statistics).activityCount(1L, ActivityType.ALL);
        verify(statistics).activity(1L, ActivityType.ALL, 0, 20);
        verify(inventory).estimateAvailable(eq(1L), eq(InventoryStatus.AVAILABLE), any());
        verify(users).findById(1L);
        verifyNoMoreInteractions(users, statistics, inventory);
    }
    @Test
    void anotherOwnerReceivesOnlyTheirOwnActivityAndUnknownSalesValuesRemainNull() throws Exception {
        when(statistics.salesTotals(2L)).thenReturn(new StatisticsRepository.SalesTotals(2, new BigDecimal("15.00"), 1));
        when(statistics.activityCount(2L, ActivityType.ITEM_SOLD)).thenReturn(2L);
        when(inventory.estimateAvailable(eq(2L), any(), any())).thenReturn(new InventoryValuation(new BigDecimal("20.00"), 3L, 2L));
        mvc.perform(get("/api/statistics/me?userId=1&type=ITEM_SOLD&page=1&size=1").header("Authorization", bearer(2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.summary.balance").value("8.00"))
                .andExpect(jsonPath("$.summary.inventoryEstimatedValue").isEmpty())
                .andExpect(jsonPath("$.summary.salesTotal").isEmpty())
                .andExpect(jsonPath("$.summary.unvaluedSales").value(1))
                .andExpect(jsonPath("$.activity.content[0].itemName").value("Other player's item"))
                .andExpect(jsonPath("$.activity.content[0].occurredAt").isEmpty());
        verify(statistics).activity(2L, ActivityType.ITEM_SOLD, 1, 1);
    }
    @Test
    void invalidFiltersAreRejectedBeforeQueryingData() throws Exception {
        for (String query : List.of("page=-1", "page=100001", "page=abc", "size=0", "size=61", "type=OPENING", "type=bad")) {
            mvc.perform(get("/api/statistics/me?" + query).header("Authorization", bearer(1)))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(users, statistics, inventory);
    }
    @Test
    void emptyAccountReturnsVerifiedZeroTotalsAndEmptyHistory() throws Exception {
        when(statistics.walletTotals(1L)).thenReturn(new StatisticsRepository.WalletTotals(0, new BigDecimal("0.00")));
        when(statistics.salesTotals(1L)).thenReturn(new StatisticsRepository.SalesTotals(0, new BigDecimal("0.00"), 0));
        when(inventory.estimateAvailable(eq(1L), any(), any())).thenReturn(new InventoryValuation(null, 0L, null));
        when(statistics.activityCount(1L, ActivityType.ALL)).thenReturn(0L);
        when(statistics.activity(1L, ActivityType.ALL, 0, 20)).thenReturn(List.of());
        mvc.perform(get("/api/statistics/me").header("Authorization", bearer(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.summary.salesTotal").value("0.00"))
                .andExpect(jsonPath("$.summary.inventoryEstimatedValue").value("0.00"))
                .andExpect(jsonPath("$.summary.openedCases").value(0))
                .andExpect(jsonPath("$.activity.totalElements").value(0))
                .andExpect(jsonPath("$.activity.content").isEmpty());
    }
}
