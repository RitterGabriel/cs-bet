package com.csbet.backend.casecatalog;

import com.csbet.backend.skin.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogImportService {
    public static final String SOURCE = "BYMYKEL";
    private final WeaponCaseRepository cases;
    private final CosmeticItemRepository items;
    private final RarityRepository rarities;
    private final CaseItemRepository links;
    public CatalogImportService(WeaponCaseRepository cases, CosmeticItemRepository items,
                                RarityRepository rarities, CaseItemRepository links) {
        this.cases = cases; this.items = items; this.rarities = rarities; this.links = links;
    }
    public record ImportSummary(int cases, int items, int associations, String revision, String language) { }

    // A leitura/validação acontece ANTES desta transação. Falhas de persistência causam rollback completo.
    @Transactional
    public ImportSummary importCatalog(SourceCatalog source) {
        Map<String, Rarity> knownRarities = rarities.findAll().stream().collect(Collectors.toMap(Rarity::getId, Function.identity()));
        Map<String, CosmeticItem> knownItems = items.findAllBySource(SOURCE).stream().collect(Collectors.toMap(CosmeticItem::getExternalId, Function.identity()));
        Map<String, WeaponCase> knownCases = cases.findAllBySource(SOURCE).stream().collect(Collectors.toMap(WeaponCase::getExternalId, Function.identity()));
        for (var input : source.items()) {
            var rarityInput = input.rarity();
            Rarity rarity = knownRarities.computeIfAbsent(rarityInput.id(),
                    id -> new Rarity(id, rarityInput.name(), rarityInput.color()));
            rarity.update(rarityInput.name(), rarityInput.color());
        }
        // saveAll também devolve as instâncias gerenciadas de raridades com ID natural.
        knownRarities = rarities.saveAll(knownRarities.values()).stream().collect(Collectors.toMap(Rarity::getId, Function.identity()));
        for (var input : source.items()) {
            CosmeticItem item = knownItems.computeIfAbsent(input.id(), id -> new CosmeticItem(SOURCE, id));
            item.update(input.name(), input.weapon(), input.weaponType(), input.specialCategory(), input.imageUrl(), knownRarities.get(input.rarity().id()));
            knownItems.put(input.id(), items.save(item));
        }
        int associations = 0;
        for (var input : source.cases()) {
            WeaponCase box = knownCases.getOrDefault(input.id(), new WeaponCase(SOURCE, input.id()));
            box.update(input.name(), input.description(), input.imageUrl(), source.revision(), source.language(), input.specialNotice());
            box = cases.save(box);
            // Atualiza apenas vínculos das caixas desta fonte presentes no lote. Não toca usuários nem coleções locais.
            links.deleteContents(box.getId());
            List<CaseItem> content = new ArrayList<>();
            for (var link : input.links()) {
                CosmeticItem item = knownItems.get(link.itemId());
                if (item == null) throw new SourceDataException("Associação sem item; importação cancelada.");
                content.add(new CaseItem(box, item, link.special()));
            }
            links.saveAll(content);
            associations += content.size();
        }
        links.flush();
        return new ImportSummary(source.cases().size(), source.items().size(), associations, source.revision(), source.language());
    }
}
