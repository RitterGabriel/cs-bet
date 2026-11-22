package com.csbet.backend.skin;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentCatalogImportService {
    private final CosmeticItemRepository items;
    private final RarityRepository rarities;
    public AgentCatalogImportService(CosmeticItemRepository items,RarityRepository rarities) { this.items=items; this.rarities=rarities; }
    @Transactional
    public int importAgents(List<AgentCatalogSource.Agent> agents) {
        var knownItems = items.findAllBySource("BYMYKEL").stream().collect(Collectors.toMap(CosmeticItem::getExternalId,item->item));
        var knownRarities = rarities.findAll().stream().collect(Collectors.toMap(Rarity::getId,rarity->rarity));
        for (var input : agents) {
            var data = input.rarity();
            var rarity = knownRarities.computeIfAbsent(data.id(),id->new Rarity(id,data.name(),data.color()));
            rarity.update(data.name(),data.color());
        }
        knownRarities = rarities.saveAll(knownRarities.values()).stream().collect(Collectors.toMap(Rarity::getId,rarity->rarity));
        for (var input : agents) {
            var item = knownItems.computeIfAbsent(input.id(),id->new CosmeticItem("BYMYKEL",id));
            item.update(input.name(),input.teamName(),AgentCatalogSource.AGENT_TYPE,"none",input.imageUrl(),knownRarities.get(input.rarity().id()));
            knownItems.put(input.id(),items.save(item));
        }
        items.flush();
        return agents.size();
    }
}
