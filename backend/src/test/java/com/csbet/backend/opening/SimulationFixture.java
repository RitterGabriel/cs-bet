package com.csbet.backend.opening;

import com.csbet.backend.casecatalog.*;
import com.csbet.backend.skin.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.test.util.ReflectionTestUtils;

final class SimulationFixture {
    static final SourceCatalog CATALOG = new CatalogSourceLoader().bundled();
    private static final Map<String, SourceCatalog.SourceItem> ITEMS = CATALOG.items().stream()
            .collect(Collectors.toMap(SourceCatalog.SourceItem::id, item -> item));
    record Pool(WeaponCase box, List<CaseItem> links) { }
    static Pool pool(String id) {
        var source = CATALOG.cases().stream().filter(box -> box.id().equals(id)).findFirst().orElseThrow();
        var box = new WeaponCase("BYMYKEL", id);
        box.update(source.name(), null, null, CATALOG.revision(), CATALOG.language(), source.specialNotice());
        ReflectionTestUtils.setField(box, "id", 1L);
        List<CaseItem> links = new ArrayList<>();
        for (var link : source.links()) {
            var data = ITEMS.get(link.itemId());
            var item = new CosmeticItem("BYMYKEL", data.id());
            item.update(data.name(), data.weapon(), data.weaponType(), data.specialCategory(), data.imageUrl(),
                    new Rarity(data.rarity().id(), data.rarity().name(), data.rarity().color()));
            ReflectionTestUtils.setField(item, "id", (long) links.size() + 1);
            links.add(new CaseItem(box, item, link.special()));
        }
        return new Pool(box, links);
    }
}
