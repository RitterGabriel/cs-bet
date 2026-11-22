package com.csbet.backend.pricing;
import com.csbet.backend.inventory.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class InventoryPricingTest {
    @Test
    void inventoryUsesBackendAggregateOfAvailableUnitsRegardlessOfPageOrFilter() {
        var inventory = mock(InventoryRepository.class); var prices = mock(PriceQueryService.class);
        when(inventory.findByOwnerIdAndStatus(eq(7L), eq(InventoryStatus.SOLD), any())).thenReturn(Page.empty());
        when(inventory.estimateAvailable(eq(7L), eq(InventoryStatus.AVAILABLE), any(java.time.LocalDateTime.class)))
                .thenReturn(new InventoryValuation(new BigDecimal("200.00"), 2L, 0L));
        var response = new InventoryService(inventory, prices, mock(com.csbet.backend.market.MarketQueryService.class), java.time.Clock.systemUTC()).findOwned(7L, 0, 12, InventoryStatus.SOLD);
        assertThat(response.estimatedAvailableTotal()).isEqualByComparingTo("200.00");
        assertThat(response.availableItems()).isEqualTo(2);
        verify(inventory).estimateAvailable(eq(7L), eq(InventoryStatus.AVAILABLE), any(java.time.LocalDateTime.class));
    }
    @Test
    void emptyInventoryIsZeroAndMissingPricesDoNotMasqueradeAsACompleteTotal() {
        assertThat(new InventoryValuation(null, 0L, null).estimatedTotal()).isEqualByComparingTo("0.00");
        assertThat(new InventoryValuation(new BigDecimal("100.00"), 2L, 1L).estimatedTotal()).isNull();
    }
    @Test
    void batchLookupAvoidsIndividualQueries() {
        var prices = mock(SimulatedPriceRepository.class);
        var service = new PriceQueryService(prices, mock(PriceHistoryRepository.class), mock(com.csbet.backend.skin.CosmeticItemRepository.class));
        when(prices.findBySkinIdIn(any())).thenReturn(List.of());
        assertThat(service.currentForIds(List.of(1L, 2L))).isEmpty();
        verify(prices).findBySkinIdIn(List.of(1L, 2L)); verify(prices, never()).findById(anyLong());
    }
}
