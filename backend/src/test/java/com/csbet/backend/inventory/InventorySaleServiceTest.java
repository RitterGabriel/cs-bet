package com.csbet.backend.inventory;
import com.csbet.backend.entity.User;
import com.csbet.backend.market.*;
import com.csbet.backend.repository.UserRepository;
import com.csbet.backend.skin.*;
import com.csbet.backend.wallet.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InventorySaleServiceTest {
    UserRepository users = mock(UserRepository.class);
    InventoryRepository inventory = mock(InventoryRepository.class);
    InventorySaleRepository sales = mock(InventorySaleRepository.class);
    MarketVariantRepository variants = mock(MarketVariantRepository.class);
    WalletMovementRepository movements = mock(WalletMovementRepository.class);
    Instant now = Instant.parse("2026-11-22T12:00:00Z");
    InventorySaleService service = new InventorySaleService(users, inventory, sales, variants, movements, Clock.fixed(now,ZoneOffset.UTC));
    User user;
    InventoryItem unit;
    MarketVariant variant;
    InventorySaleRequest request = new InventorySaleRequest(UUID.randomUUID(),new BigDecimal("12.50"));
    @BeforeEach void setup() {
        user = new User("test","test@example.invalid","unused",new BigDecimal("10.00"),LocalDateTime.now());
        var skin = new CosmeticItem("BYMYKEL","skin");
        skin.update("Test skin","Test","rifles","none",null,new Rarity("rare","Rare","#4b69ff")); ReflectionTestUtils.setField(skin,"id",1L);
        variant = mock(MarketVariant.class); when(variant.getId()).thenReturn(2L); when(variant.getSkinId()).thenReturn(1L);
        when(variant.getWear()).thenReturn(Wear.FT); when(variant.getCurrency()).thenReturn("BRL");
        when(variant.getPriceAmount()).thenReturn(request.expectedPrice()); when(variant.getValidUntil()).thenReturn(LocalDateTime.ofInstant(now.plusSeconds(3600),ZoneOffset.UTC));
        unit = new InventoryItem(user,skin,variant,null); ReflectionTestUtils.setField(unit,"id",5L); ReflectionTestUtils.setField(unit,"obtainedAt",LocalDateTime.ofInstant(now.minusSeconds(3600),ZoneOffset.UTC));
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user)); when(inventory.findOwnedForUpdate(5L,1L)).thenReturn(Optional.of(unit));
        when(variants.findById(2L)).thenReturn(Optional.of(variant)); when(movements.saveAndFlush(any())).thenAnswer(c -> c.getArgument(0));
        when(sales.saveAndFlush(any())).thenAnswer(c -> { InventorySale sale=c.getArgument(0); ReflectionTestUtils.setField(sale,"id",8L); return sale; });
    }
    @Test void saleCreditsExactServerQuoteAndFreezesSnapshot() {
        var result=service.sell(1L,5L,request);
        assertThat(result.amount()).isEqualByComparingTo("12.50"); assertThat(result.balance()).isEqualByComparingTo("22.50");
        assertThat(unit.getStatus()).isEqualTo(InventoryStatus.SOLD); assertThat(unit.getSalePrice()).isEqualByComparingTo("12.50");
        assertThat(unit.getSoldAt()).isEqualTo(LocalDateTime.ofInstant(now,ZoneOffset.UTC));
        verify(movements).saveAndFlush(argThat(m -> m.getType().equals("ITEM_SALE") && m.getResultingBalance().compareTo(result.balance())==0));
    }
    @Test void retriesReturnSameReceiptWithoutAnotherCreditOrQuoteLookup() {
        var result=service.sell(1L,5L,request);
        var saved=org.mockito.ArgumentCaptor.forClass(InventorySale.class); verify(sales).saveAndFlush(saved.capture());
        when(sales.findByUserIdAndRequestId(1L,request.requestId().toString())).thenReturn(Optional.of(saved.getValue()));
        clearInvocations(inventory,variants,movements);
        assertThat(service.sell(1L,5L,request)).isEqualTo(result); verifyNoInteractions(inventory,variants,movements);
        assertThat(user.getBalance()).isEqualByComparingTo("22.50");
    }
    @Test void anotherUuidCannotSellTheSameUnitTwice() {
        service.sell(1L,5L,request);
        assertThatThrownBy(() -> service.sell(1L,5L,new InventorySaleRequest(UUID.randomUUID(),request.expectedPrice()))).hasMessageContaining("já foi vendida");
        assertThat(user.getBalance()).isEqualByComparingTo("22.50"); verify(sales,times(1)).saveAndFlush(any());
    }
    @Test void cannotSellAnotherOwnersUnit() {
        assertThatThrownBy(() -> service.sell(1L,99L,request)).isInstanceOf(com.csbet.backend.casecatalog.CatalogNotFoundException.class);
        verifyNoInteractions(variants,movements); assertThat(user.getBalance()).isEqualByComparingTo("10.00");
    }
    @Test void staleOrChangedPricesNeverCreditClientSuppliedAmount() {
        when(variant.getPriceAmount()).thenReturn(new BigDecimal("13.00"));
        assertThatThrownBy(() -> service.sell(1L,5L,request)).hasMessageContaining("preço mudou");
        when(variant.getValidUntil()).thenReturn(LocalDateTime.ofInstant(now.minusSeconds(1),ZoneOffset.UTC));
        assertThatThrownBy(() -> service.sell(1L,5L,request)).hasMessageContaining("sem cotação");
        assertThat(unit.getStatus()).isEqualTo(InventoryStatus.AVAILABLE); verify(movements,never()).saveAndFlush(any());
        assertThat(user.getBalance()).isEqualByComparingTo("10.00");
    }
    @Test void legacyUnitWithoutIdentifiedVariantIsNotGivenAnInventedPrice() {
        ReflectionTestUtils.setField(unit,"marketVariantId",null);
        assertThatThrownBy(() -> service.sell(1L,5L,request)).hasMessageContaining("variante identificada");
        verifyNoInteractions(variants); verify(movements,never()).saveAndFlush(any()); assertThat(unit.getStatus()).isEqualTo(InventoryStatus.AVAILABLE);
    }
}
