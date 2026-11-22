package com.csbet.backend.opening;

import com.csbet.backend.casecatalog.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SimulationPreviewServiceTest {
    private final WeaponCaseRepository cases = mock(WeaponCaseRepository.class);
    private final CaseItemRepository contents = mock(CaseItemRepository.class);
    private final OfficialCaseOdds odds = new OfficialCaseOdds();
    private final RandomGenerator random = mock(RandomGenerator.class);
    private final RandomGenerator specialRandom = mock(RandomGenerator.class);
    private final SpecialRewardResolver resolver = new SpecialRewardResolver(cases, contents, odds, specialRandom);
    private final SimulationPreviewService service = new SimulationPreviewService(cases, contents, odds, resolver, random);

    private SimulationFixture.Pool load(String id) {
        var pool = SimulationFixture.pool(id);
        when(cases.findBySourceAndExternalId("BYMYKEL", id)).thenReturn(Optional.of(pool.box()));
        when(contents.findContents(1L)).thenReturn(pool.links());
        return pool;
    }
    private SimulationPreviewResponse preview(String id, int samples) {
        return service.preview(new SimulationPreviewRequest(id, samples));
    }

    @Test
    void nonexistentCaseIsNotSilentlySimulated() {
        assertThatThrownBy(() -> preview("missing", 1)).isInstanceOf(CatalogNotFoundException.class);
        verifyNoInteractions(random, specialRandom, contents);
    }

    @Test
    void aCompleteCycleOfIntegerTicketsMatchesEveryTheoreticalWeightExactly() {
        var pool = load("crate-7007");
        var ticket = new AtomicInteger();
        when(random.nextInt(782)).thenAnswer(call -> ticket.getAndIncrement());
        var result = preview("crate-7007", 782);
        assertThat(result.status()).isEqualTo("AVAILABLE");
        assertThat(result.requestedSamples()).isEqualTo(782);
        assertThat(result.totalSamples()).isEqualTo(782);
        assertThat(result.rarities()).extracting(SimulationPreviewResponse.RarityCount::count)
                .containsExactly(625, 125, 25, 5, 2);
        assertThat(result.rarities()).allSatisfy(rarity -> {
            assertThat(rarity.observedPercent()).isEqualByComparingTo(rarity.theoreticalChance().percent());
            assertThat(rarity.theoreticalChance().sourceUrl()).isEqualTo(OfficialCaseOdds.SOURCE);
        });
        assertThat(result.items().stream().mapToInt(SimulationPreviewResponse.ItemCount::count).sum()).isEqualTo(782);
        var allowed = pool.links().stream().map(link -> link.getItem().getExternalId()).toList();
        assertThat(result.items()).allSatisfy(item -> {
            assertThat(allowed).contains(item.externalId());
            assertThat(item.variant().status()).isEqualTo("UNDETERMINED");
            assertThat(item.variant().wear()).isNull();
            assertThat(item.variant().statTrak()).isNull();
            assertThat(item.variant().phase()).isNull();
        });
        // Somente duas consultas, independentemente do número de amostras; nenhuma gravação.
        verify(cases).findBySourceAndExternalId("BYMYKEL", "crate-7007");
        verify(contents).findContents(1L);
        verifyNoMoreInteractions(cases, contents);
        verify(specialRandom, times(2)).nextInt(52);
    }

    @Test
    void cumulativeBoundariesAssignEveryTicketToTheCorrectRarity() {
        load("crate-7007");
        when(random.nextInt(782)).thenReturn(0, 624, 625, 749, 750, 774, 775, 779, 780, 781);
        var result = preview("crate-7007", 10);
        assertThat(result.rarities()).extracting(SimulationPreviewResponse.RarityCount::count)
                .containsExactly(2, 2, 2, 2, 2);
        assertThat(result.rarities()).allSatisfy(rarity ->
                assertThat(rarity.observedPercent()).isEqualByComparingTo("20"));
    }

    @Test
    void ordinaryItemsAreUniformWithinEachRarityAndNeverComeFromAnotherPool() {
        var pool = load("crate-7007");
        var distribution = odds.distributionForCase(pool.box(), pool.links());
        int ticket = 0;
        for (var rarity : distribution) {
            if (rarity.rarityId().equals(OfficialCaseOdds.SPECIAL_GROUP)) continue;
            var expected = pool.links().stream().filter(link -> !link.isSpecial())
                    .map(CaseItem::getItem).filter(item -> item.getRarity().getId().equals(rarity.rarityId()))
                    .map(com.csbet.backend.skin.CosmeticItem::getExternalId).toList();
            when(random.nextInt(782)).thenReturn(ticket);
            var index = new AtomicInteger();
            when(random.nextInt(expected.size())).thenAnswer(call -> index.getAndIncrement());
            var result = preview("crate-7007", expected.size());
            assertThat(result.items()).extracting(SimulationPreviewResponse.ItemCount::externalId)
                    .containsExactlyInAnyOrderElementsOf(expected);
            assertThat(result.items()).allSatisfy(item -> {
                assertThat(item.count()).isEqualTo(1);
                assertThat(item.special()).isFalse();
                assertThat(item.officialItemProbabilityVerified()).isTrue();
            });
            ticket += rarity.weight();
        }
        verifyNoInteractions(specialRandom);
    }

    @Test
    void specialDrawsReuseOneOverNAndPreserveAllCatalogIdsIncludingPhaseEntries() {
        var pool = load("crate-7007");
        var expected = pool.links().stream().filter(CaseItem::isSpecial)
                .map(link -> link.getItem().getExternalId()).toList();
        when(random.nextInt(782)).thenReturn(781);
        var index = new AtomicInteger();
        when(specialRandom.nextInt(expected.size())).thenAnswer(call -> index.getAndIncrement());
        var result = preview("crate-7007", expected.size());
        assertThat(expected).hasSize(52);
        assertThat(result.items()).extracting(SimulationPreviewResponse.ItemCount::externalId)
                .containsExactlyInAnyOrderElementsOf(expected);
        assertThat(result.items()).allSatisfy(item -> {
            assertThat(item.count()).isEqualTo(1);
            assertThat(item.special()).isTrue();
            assertThat(item.selectionBasis()).isEqualTo(CatalogDtos.SpecialRewardGroup.UNIFORM_RULE);
            assertThat(item.officialItemProbabilityVerified()).isFalse();
            assertThat(item.variant().phase()).isNull();
        });
        assertThat(result.rarities().getLast().scope()).isEqualTo("SPECIAL_GROUP");
        assertThat(result.rarities().getLast().observedPercent()).isEqualByComparingTo("100");
    }

    @Test
    void allFortyTwoCasesUseOnlyTheirOwnSpecialPoolsWithoutQueriesPerDraw() {
        when(random.nextInt(782)).thenReturn(781);
        for (var source : SimulationFixture.CATALOG.cases()) {
            var pool = load(source.id());
            var result = preview(source.id(), 1);
            assertThat(result.status()).isEqualTo("AVAILABLE");
            assertThat(pool.links().stream().filter(CaseItem::isSpecial).map(link -> link.getItem().getId()))
                    .contains(result.items().getFirst().skinId());
        }
    }

    @Test
    void incompleteOrdinaryOrSpecialPoolsAndDuplicateLinksAreUnavailableWithoutDrawing() {
        var pool = load("crate-7007");
        for (boolean special : List.of(false, true)) {
            var partial = new ArrayList<>(pool.links());
            partial.removeIf(link -> link.isSpecial() == special && link == pool.links().stream()
                    .filter(candidate -> candidate.isSpecial() == special).findFirst().orElseThrow());
            when(contents.findContents(1L)).thenReturn(partial);
            assertUnavailable("UNVERIFIED_OR_INCOMPLETE_CATALOG");
        }
        var duplicated = new ArrayList<>(pool.links()); duplicated.add(pool.links().getFirst());
        when(contents.findContents(1L)).thenReturn(duplicated);
        assertUnavailable("UNVERIFIED_OR_INCOMPLETE_CATALOG");
        when(contents.findContents(1L)).thenReturn(List.of());
        assertUnavailable("UNVERIFIED_OR_INCOMPLETE_CATALOG");
        verifyNoInteractions(random, specialRandom);
    }

    @Test
    void unverifiedRevisionAndInvalidDatabaseIdsAreUnavailableRatherThanInventingOdds() {
        var pool = load("crate-7007");
        ReflectionTestUtils.setField(pool.box(), "sourceRevision", "0".repeat(40));
        assertUnavailable("UNVERIFIED_OR_INCOMPLETE_CATALOG");
        ReflectionTestUtils.setField(pool.box(), "sourceRevision", SimulationFixture.CATALOG.revision());
        ReflectionTestUtils.setField(pool.links().getFirst().getItem(), "id", null);
        assertUnavailable("INCONSISTENT_ITEM_IDENTIFIERS");
        verifyNoInteractions(random, specialRandom);
    }

    private void assertUnavailable(String reason) {
        var result = preview("crate-7007", 100);
        assertThat(result.status()).isEqualTo("UNAVAILABLE");
        assertThat(result.reason()).isEqualTo(reason);
        assertThat(result.totalSamples()).isZero();
        assertThat(result.items()).isEmpty();
        assertThat(result.rarities()).isEmpty();
    }
}
