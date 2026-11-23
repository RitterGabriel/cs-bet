package com.csbet.backend.opening;
import com.csbet.backend.entity.User;
import com.csbet.backend.repository.UserRepository;
import com.csbet.backend.wallet.WalletMovementRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OpeningBatchServiceTest {
    UserRepository users = mock(UserRepository.class);
    OpeningBatchRepository batches = mock(OpeningBatchRepository.class);
    CaseOpeningService opening = mock(CaseOpeningService.class);
    WalletMovementRepository movements = mock(WalletMovementRepository.class);
    User user = new User("test", "test@example.invalid", "unused", new BigDecimal("100.00"), LocalDateTime.now());
    OpeningBatchService service = new OpeningBatchService(users, batches, opening, movements, Clock.systemUTC());
    @BeforeEach void setup() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(batches.saveAndFlush(any())).thenAnswer(call -> { OpeningBatch b = call.getArgument(0); ReflectionTestUtils.setField(b,"id", 4L); return b; });
        when(opening.open(eq(1L), any())).thenAnswer(call -> new OpeningResponse(1L,1L,"case",new BigDecimal("2.00"),new BigDecimal("98.00"),null,null,null));
    }
    @ParameterizedTest @ValueSource(ints = {1,2,5,10})
    void supportedCountsProduceOneOpeningPerUnitWithDistinctStableIdentifiers(int quantity) {
        var request = new OpeningBatchRequest("case", UUID.randomUUID(), new BigDecimal("2.00"), quantity);
        var response = service.open(1L, request);
        assertThat(response.results()).hasSize(quantity); assertThat(response.charged()).isEqualByComparingTo(BigDecimal.valueOf(quantity * 2L));
        var ids = org.mockito.ArgumentCaptor.forClass(OpeningRequest.class);
        verify(opening, times(quantity)).open(eq(1L), ids.capture());
        assertThat(ids.getAllValues().stream().map(OpeningRequest::requestId)).doesNotHaveDuplicates();
        var saved = org.mockito.ArgumentCaptor.forClass(OpeningBatch.class); verify(batches).saveAndFlush(saved.capture());
        when(batches.findByUserIdAndRequestId(1L, request.requestId().toString())).thenReturn(Optional.of(saved.getValue()));
        clearInvocations(opening, batches);
        service.open(1L,request);
        var repeated = org.mockito.ArgumentCaptor.forClass(OpeningRequest.class);
        verify(opening, times(quantity)).open(eq(1L), repeated.capture());
        assertThat(repeated.getAllValues()).containsExactlyElementsOf(ids.getAllValues());
        verify(batches, never()).saveAndFlush(any());
    }
    @Test void insufficientTotalBalanceRefusesBeforeAnyOpening() {
        assertThatThrownBy(() -> service.open(1L,new OpeningBatchRequest("case",UUID.randomUUID(),new BigDecimal("60.00"),2)))
            .hasMessageContaining("insuficiente");
        verifyNoInteractions(opening); verify(batches,never()).saveAndFlush(any());
    }
    @Test void aRequestCannotBeReusedWithAnotherQuantityOrPrice() {
        var first = new OpeningBatchRequest("case",UUID.randomUUID(),new BigDecimal("2.00"),2);
        var batch = new OpeningBatch(user,first,Instant.now());
        when(batches.findByUserIdAndRequestId(1L, first.requestId().toString())).thenReturn(Optional.of(batch));
        assertThatThrownBy(() -> service.open(1L,new OpeningBatchRequest("case",first.requestId(),first.expectedPrice(),5)))
            .hasMessageContaining("outro lote");
        verifyNoInteractions(opening);
    }
    @ParameterizedTest @ValueSource(ints = {-1,0,3,11,100})
    void refusesUnsupportedQuantities(int quantity) {
        assertThatThrownBy(() -> service.open(1L,new OpeningBatchRequest("case",UUID.randomUUID(),new BigDecimal("2.00"),quantity)))
            .isInstanceOf(OpeningUnavailableException.class);
        verifyNoInteractions(users, opening, batches);
    }
}
