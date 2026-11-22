package com.csbet.backend.casecatalog;

import com.csbet.backend.skin.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CatalogImportServiceTest {
    @Test
    void repeatedImportUpdatesWithoutDuplicatingItemsCasesOrLinks() {
        var cases = mock(WeaponCaseRepository.class);
        var items = mock(CosmeticItemRepository.class);
        var rarities = mock(RarityRepository.class);
        var links = mock(CaseItemRepository.class);
        Map<String, WeaponCase> caseStore = new HashMap<>();
        Map<String, CosmeticItem> itemStore = new HashMap<>();
        Map<String, Rarity> rarityStore = new HashMap<>();
        Map<Long, List<CaseItem>> linkStore = new HashMap<>();
        AtomicLong sequence = new AtomicLong();
        when(cases.findAllBySource(CatalogImportService.SOURCE)).thenAnswer(call -> new ArrayList<>(caseStore.values()));
        when(items.findAllBySource(CatalogImportService.SOURCE)).thenAnswer(call -> new ArrayList<>(itemStore.values()));
        when(rarities.findAll()).thenAnswer(call -> new ArrayList<>(rarityStore.values()));
        when(rarities.saveAll(any())).thenAnswer(call -> {
            List<Rarity> result = new ArrayList<>();
            for (Rarity rarity : call.<Iterable<Rarity>>getArgument(0)) { rarityStore.put(rarity.getId(), rarity); result.add(rarity); }
            return result;
        });
        when(cases.save(any())).thenAnswer(call -> {
            WeaponCase box = call.getArgument(0);
            if (box.getId() == null) ReflectionTestUtils.setField(box, "id", sequence.incrementAndGet());
            caseStore.put(box.getExternalId(), box); return box;
        });
        when(items.save(any())).thenAnswer(call -> {
            CosmeticItem item = call.getArgument(0);
            if (item.getId() == null) ReflectionTestUtils.setField(item, "id", sequence.incrementAndGet());
            itemStore.put(item.getExternalId(), item); return item;
        });
        doAnswer(call -> { linkStore.put(call.getArgument(0), new ArrayList<>()); return null; }).when(links).deleteContents(anyLong());
        when(links.saveAll(any())).thenAnswer(call -> {
            List<CaseItem> result = new ArrayList<>();
            for (CaseItem link : call.<Iterable<CaseItem>>getArgument(0)) {
                WeaponCase box = (WeaponCase) ReflectionTestUtils.getField(link, "weaponCase");
                linkStore.get(box.getId()).add(link); result.add(link);
            }
            return result;
        });
        var service = new CatalogImportService(cases, items, rarities, links);
        var source = new CatalogSourceLoader().bundled();
        var first = service.importCatalog(source);
        Map<String, Long> ids = new HashMap<>();
        caseStore.forEach((key, box) -> ids.put(key, box.getId()));
        var second = service.importCatalog(source);
        assertThat(first).isEqualTo(second);
        assertThat(caseStore).hasSize(42);
        assertThat(itemStore).hasSize(1305);
        assertThat(linkStore.values().stream().mapToInt(List::size).sum()).isEqualTo(2508);
        caseStore.forEach((key, box) -> assertThat(box.getId()).isEqualTo(ids.get(key)));
        var box = source.cases().getFirst();
        var updated = new SourceCatalog.SourceCase(box.id(), box.name() + " (updated)", box.description(), box.imageUrl(), box.links().subList(1, box.links().size()), box.specialNotice());
        service.importCatalog(new SourceCatalog(List.of(updated), source.items(), source.revision(), source.language()));
        assertThat(caseStore.get(box.id()).getName()).endsWith("(updated)");
        assertThat(caseStore).hasSize(42);
        assertThat(itemStore).hasSize(1305);
        assertThat(linkStore.values().stream().mapToInt(List::size).sum()).isEqualTo(2507);
        // Nunca executa limpeza global: somente associações das caixas do próprio lote.
        verify(cases, never()).deleteAll();
        verify(items, never()).deleteAll();
        verify(links, never()).deleteAll();
    }
}
