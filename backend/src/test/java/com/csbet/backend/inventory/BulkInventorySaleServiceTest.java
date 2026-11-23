package com.csbet.backend.inventory;
import com.csbet.backend.entity.User;
import com.csbet.backend.market.*;
import com.csbet.backend.repository.UserRepository;
import com.csbet.backend.skin.CosmeticItem;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class BulkInventorySaleServiceTest {
    UserRepository users=mock(UserRepository.class);
    InventoryRepository inventory=mock(InventoryRepository.class);
    MarketVariantRepository variants=mock(MarketVariantRepository.class);
    InventorySaleService sales=mock(InventorySaleService.class);
    InventorySaleBatchRepository batches=mock(InventorySaleBatchRepository.class);
    Instant now=Instant.parse("2026-11-22T12:00:00Z");
    BulkInventorySaleService service=new BulkInventorySaleService(users,inventory,variants,sales,batches,Clock.fixed(now,ZoneOffset.UTC));
    User user=new User("test","test@example.invalid","unused",new BigDecimal("100.00"),LocalDateTime.now());
    @BeforeEach void setup() {
        when(users.findById(1L)).thenReturn(Optional.of(user));when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(batches.saveAndFlush(any())).thenAnswer(c->{InventorySaleBatch batch=c.getArgument(0);ReflectionTestUtils.setField(batch,"id",9L);return batch;});
    }
    InventoryItem unit(long id,Long variantId) {
        var unit=mock(InventoryItem.class);var skin=mock(CosmeticItem.class);when(skin.getId()).thenReturn(10L);
        when(unit.getId()).thenReturn(id);when(unit.getMarketVariantId()).thenReturn(variantId);
        when(unit.getWear()).thenReturn(Wear.FT);when(unit.getStatTrak()).thenReturn(false);when(unit.getSkin()).thenReturn(skin);return unit;
    }
    MarketVariant quote(long id,boolean fresh) {
        var variant=mock(MarketVariant.class);when(variant.getId()).thenReturn(id);when(variant.getSkinId()).thenReturn(10L);
        when(variant.getWear()).thenReturn(Wear.FT);when(variant.getCurrency()).thenReturn("BRL");when(variant.getPriceAmount()).thenReturn(new BigDecimal("12.50"));
        when(variant.getValidUntil()).thenReturn(LocalDateTime.ofInstant(now.plusSeconds(fresh?3600:-1),ZoneOffset.UTC));return variant;
    }
    BulkSaleDtos.Request request() { return new BulkSaleDtos.Request(UUID.randomUUID(),List.of(new BulkSaleDtos.Line(2L,new BigDecimal("5.00")),new BulkSaleDtos.Line(1L,new BigDecimal("12.50")))); }
    @Test void previewIncludesAllPagesAndSkipsLegacyOrUnpricedUnitsWithoutMutatingThem() {
        var units=List.of(unit(1,7L),unit(2,null),unit(3,8L));
        var priced=List.of(quote(7,true),quote(8,false));
        when(inventory.findByOwnerIdAndStatus(eq(1L),eq(InventoryStatus.AVAILABLE),any())).thenReturn(new PageImpl<>(units));
        when(variants.findAllById(any())).thenReturn(priced);
        var result=service.quote(1L);assertThat(result.items()).containsExactly(new BulkSaleDtos.Line(1L,new BigDecimal("12.50")));
        assertThat(result.skippedItems()).isEqualTo(2);assertThat(result.total()).isEqualByComparingTo("12.50");
        verifyNoInteractions(sales,batches);
        verify(inventory).findByOwnerIdAndStatus(eq(1L),eq(InventoryStatus.AVAILABLE),argThat(p->p.getPageNumber()==0&&p.getPageSize()==2001));
    }
    @Test void entireManifestIsIdempotentEvenWhenOrderChangesAndEachUnitHasItsOwnReceipt() {
        when(sales.sell(eq(1L),anyLong(),any())).thenAnswer(c->{Long id=c.getArgument(1);InventorySaleRequest r=c.getArgument(2);
            return new InventorySaleResponse(id,id,r.expectedPrice(),id==1?new BigDecimal("112.50"):new BigDecimal("117.50"));});
        var request=request();var result=service.sell(1L,request);
        assertThat(result.soldItems()).isEqualTo(2);assertThat(result.amount()).isEqualByComparingTo("17.50");
        assertThat(result.balance()).isEqualByComparingTo("117.50");
        var saved=org.mockito.ArgumentCaptor.forClass(InventorySaleBatch.class);verify(batches).saveAndFlush(saved.capture());
        var ids=org.mockito.ArgumentCaptor.forClass(InventorySaleRequest.class);verify(sales,times(2)).sell(eq(1L),anyLong(),ids.capture());
        assertThat(ids.getAllValues().stream().map(InventorySaleRequest::requestId)).doesNotHaveDuplicates();
        when(batches.findByUserIdAndRequestId(1L,request.requestId().toString())).thenReturn(Optional.of(saved.getValue()));
        clearInvocations(sales);
        assertThat(service.sell(1L,new BulkSaleDtos.Request(request.requestId(),request.items().reversed()))).isEqualTo(result);verifyNoInteractions(sales);
        assertThatThrownBy(()->service.sell(1L,new BulkSaleDtos.Request(request.requestId(),List.of(request.items().getFirst())))).hasMessageContaining("outro conjunto");
    }
    @Test void rejectsDuplicateUnitsBeforeAnyMutation() {
        var line=new BulkSaleDtos.Line(1L,new BigDecimal("1.00"));
        assertThatThrownBy(()->service.sell(1L,new BulkSaleDtos.Request(UUID.randomUUID(),List.of(line,line)))).isInstanceOf(InventorySaleException.class);
        verifyNoInteractions(sales,batches,users);
    }
    @Test void unknownOwnerCannotQuoteOrSell() {
        assertThatThrownBy(()->service.quote(2L)).isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
        assertThatThrownBy(()->service.sell(2L,request())).isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
        verifyNoInteractions(sales,inventory,variants,batches);
    }
}
