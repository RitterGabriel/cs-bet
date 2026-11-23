package com.csbet.backend.opening;

import com.csbet.backend.casecatalog.*;
import com.csbet.backend.entity.User;
import com.csbet.backend.inventory.*;
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

class CaseOpeningServiceTest {
    UserRepository users = mock(UserRepository.class);
    WeaponCaseRepository cases = mock(WeaponCaseRepository.class);
    CosmeticItemRepository skins = mock(CosmeticItemRepository.class);
    SimulationPreviewService simulation = mock(SimulationPreviewService.class);
    MarketVariantRepository variants = mock(MarketVariantRepository.class);
    InventoryRepository inventory = mock(InventoryRepository.class);
    WalletMovementRepository movements = mock(WalletMovementRepository.class);
    CaseOpeningRepository openings = mock(CaseOpeningRepository.class);
    final Instant now = Instant.parse("2026-11-22T12:00:00Z");
    CaseOpeningService service = new CaseOpeningService(users, cases, skins, simulation, variants, new VariantSelector(),
            inventory, movements, openings, Clock.fixed(now, ZoneOffset.UTC), mock(CustomCaseService.class));
    User owner;
    WeaponCase box;
    CosmeticItem skin;
    MarketVariant price;
    MarketVariant variant;
    OpeningRequest request = new OpeningRequest("crate-7007", UUID.randomUUID(), new BigDecimal("2.71"));
    @BeforeEach void setup() {
        owner = new User("owner", "owner@example.invalid", "unused", new BigDecimal("10.00"), LocalDateTime.now());
        ReflectionTestUtils.setField(owner, "id", 1L);
        var pool = SimulationFixture.pool("crate-7007"); box = pool.box(); skin = pool.links().stream().filter(l -> !l.isSpecial()).findFirst().orElseThrow().getItem();
        price = mock(MarketVariant.class); when(price.getCurrency()).thenReturn("BRL"); when(price.getPriceAmount()).thenReturn(request.expectedPrice());
        when(price.getValidUntil()).thenReturn(LocalDateTime.ofInstant(now.plusSeconds(3600), ZoneOffset.UTC));
        variant = mock(MarketVariant.class); when(variant.getId()).thenReturn(90L); when(variant.getSkinId()).thenReturn(skin.getId());
        when(variant.getWear()).thenReturn(Wear.FT); when(variant.isStatTrak()).thenReturn(true);
        when(variant.getCurrency()).thenReturn("BRL"); when(variant.getPriceAmount()).thenReturn(new BigDecimal("12.50"));
        when(variant.getValidUntil()).thenReturn(LocalDateTime.ofInstant(now.plusSeconds(3600), ZoneOffset.UTC));
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(owner));
        when(cases.findBySourceAndExternalId("BYMYKEL", request.caseId())).thenReturn(Optional.of(box));
        when(variants.findByCaseIdIn(List.of(box.getId()))).thenReturn(List.of(price));
        when(variants.findBySkinIdIn(List.of(skin.getId()))).thenReturn(List.of(variant));
        when(skins.findById(skin.getId())).thenReturn(Optional.of(skin));
        var rarity = skin.getRarity();
        var winner = new SimulationPreviewResponse.ItemCount(skin.getId(), skin.getExternalId(), skin.getName(),
                new SimulationPreviewResponse.Rarity(rarity.getId(), rarity.getName(), rarity.getColor()), false, 1, new BigDecimal("100"),
                "OFFICIAL_EQUAL_WITHIN_RARITY", true, new SimulationPreviewResponse.Variant("UNDETERMINED", null, null, null, "reason"));
        when(simulation.preview(any())).thenReturn(new SimulationPreviewResponse("AVAILABLE", null, request.caseId(), 1, 1, List.of(), List.of(winner), List.of()));
        when(inventory.saveAndFlush(any())).thenAnswer(call -> { InventoryItem unit = call.getArgument(0); ReflectionTestUtils.setField(unit,"id", 50L); return unit; });
        when(movements.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(openings.saveAndFlush(any())).thenAnswer(call -> { CaseOpening opening = call.getArgument(0); ReflectionTestUtils.setField(opening,"id", 80L); return opening; });
    }
    @Test void chargesServerPriceAndPersistsTheExactChosenSkinAndVariant() {
        var result = service.open(1L, request);
        assertThat(result.balance()).isEqualByComparingTo("7.29"); assertThat(result.charged()).isEqualByComparingTo("2.71");
        assertThat(result.referencePrice()).isEqualByComparingTo("12.50");
        assertThat(result.item().externalId()).isEqualTo(skin.getExternalId());
        assertThat(result.inventoryId()).isEqualTo(50L); assertThat(result.variant().wear()).isEqualTo(Wear.FT);
        assertThat(result.variant().statTrak()).isTrue();
        var order = inOrder(users, inventory, movements, openings);
        order.verify(users).findByIdForUpdate(1L);
        order.verify(inventory).saveAndFlush(argThat(i -> i.getMarketVariantId().equals(90L) && i.getSkin() == skin));
        order.verify(movements).saveAndFlush(argThat(m -> m.getType().equals("CASE_OPENING") && m.getResultingBalance().compareTo(result.balance()) == 0));
        order.verify(openings).saveAndFlush(any());
    }
    @Test void replayReturnsThePersistedResultWithoutDrawingOrChargingAgain() {
        var first = service.open(1L, request);
        var saved = org.mockito.ArgumentCaptor.forClass(CaseOpening.class); verify(openings).saveAndFlush(saved.capture());
        clearInvocations(simulation, inventory, movements, variants);
        when(openings.findByUserIdAndRequestId(1L, request.requestId().toString())).thenReturn(Optional.of(saved.getValue()));
        assertThat(service.open(1L, request)).isEqualTo(first);
        verifyNoInteractions(simulation, inventory, movements, variants);
        assertThat(owner.getBalance()).isEqualByComparingTo("7.29");
        assertThatThrownBy(() -> service.open(1L, new OpeningRequest("other-case", request.requestId(), request.expectedPrice())))
                .isInstanceOf(OpeningUnavailableException.class);
    }
    @Test void insufficientBalanceIsRejectedBeforeAnyDrawOrWrite() {
        owner.spendCredits(new BigDecimal("9.00"));
        assertThatThrownBy(() -> service.open(1L, request)).hasMessageContaining("insuficiente");
        verifyNoInteractions(simulation, inventory); verify(movements, never()).saveAndFlush(any());
        assertThat(owner.getBalance()).isEqualByComparingTo("1.00");
    }
    @Test void changedOrMissingCaseQuoteNeverChargesTheClientAmount() {
        when(price.getPriceAmount()).thenReturn(new BigDecimal("3.00"));
        assertThatThrownBy(() -> service.open(1L, request)).hasMessageContaining("mudou");
        when(price.getValidUntil()).thenReturn(LocalDateTime.ofInstant(now.minusSeconds(1), ZoneOffset.UTC));
        assertThatThrownBy(() -> service.open(1L, request)).hasMessageContaining("indisponível");
        verifyNoInteractions(simulation, inventory); assertThat(owner.getBalance()).isEqualByComparingTo("10.00");
    }
    @Test void missingVariantDoesNotDebitOrWriteAndDoesNotReroll() {
        when(variants.findBySkinIdIn(any())).thenReturn(List.of());
        assertThatThrownBy(() -> service.open(1L, request)).hasMessageContaining("variantes");
        verify(simulation).preview(new SimulationPreviewRequest(request.caseId(), 1));
        verifyNoInteractions(inventory); assertThat(owner.getBalance()).isEqualByComparingTo("10.00");
    }
    @Test void staleSkinQuoteDoesNotExcludeTheVariantOrInventAPrice() {
        when(variant.getValidUntil()).thenReturn(LocalDateTime.ofInstant(now.minusSeconds(1), ZoneOffset.UTC));
        var result = service.open(1L, request);
        assertThat(result.referencePrice()).isNull(); assertThat(result.variant().wear()).isEqualTo(Wear.FT);
    }
    @Test void deletedOrOtherUserCannotAccessThisWallet() {
        assertThatThrownBy(() -> service.open(2L, request)).isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
        verifyNoInteractions(simulation, inventory, movements, openings);
    }
}
