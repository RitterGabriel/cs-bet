package com.csbet.backend.opening;

import com.csbet.backend.casecatalog.*;
import com.csbet.backend.skin.CosmeticItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import static com.csbet.backend.opening.SimulationPreviewResponse.*;

@Service
@Validated
public class SimulationPreviewService {
    private static final List<String> LIMITATIONS = List.of(
            "As probabilidades oficiais abrangem raridades e o grupo especial, não variantes individuais.",
            "Itens comuns da mesma raridade são equiprováveis; especiais usam a regra interna CS Bet 1/N por ID do catálogo.",
            "IDs especiais podem distinguir fases. A regra 1/N não comprova probabilidades oficiais de fases ou acabamentos.",
            "Desgaste, StatTrak e fase não são determinados: faltam distribuições verificáveis para este motor.",
            "Frequências observadas podem divergir das probabilidades teóricas em amostras finitas.");
    private static final Variant UNDETERMINED_VARIANT = new Variant("UNDETERMINED", null, null, null,
            "VERIFIABLE_VARIANT_DISTRIBUTION_UNAVAILABLE");
    private final WeaponCaseRepository cases;
    private final CaseItemRepository contents;
    private final OfficialCaseOdds odds;
    private final SpecialRewardResolver specialResolver;
    private final RandomGenerator random;

    @Autowired
    public SimulationPreviewService(WeaponCaseRepository cases, CaseItemRepository contents,
                                    OfficialCaseOdds odds, SpecialRewardResolver specialResolver) {
        this(cases, contents, odds, specialResolver, new SecureRandom());
    }

    SimulationPreviewService(WeaponCaseRepository cases, CaseItemRepository contents,
                             OfficialCaseOdds odds, SpecialRewardResolver specialResolver, RandomGenerator random) {
        this.cases = cases;
        this.contents = contents;
        this.odds = odds;
        this.specialResolver = specialResolver;
        this.random = random;
    }

    // Um snapshot consistente impede misturar partes do catálogo durante uma importação concorrente.
    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public SimulationPreviewResponse preview(@NotNull @Valid SimulationPreviewRequest request) {
        var box = cases.findBySourceAndExternalId("BYMYKEL", request.caseId())
                .orElseThrow(CatalogNotFoundException::new);
        var links = contents.findContents(box.getId());
        var distribution = odds.distributionForCase(box, links);
        if (distribution.isEmpty()) return unavailable(request, "UNVERIFIED_OR_INCOMPLETE_CATALOG");
        if (links.stream().anyMatch(link -> link.getItem().getId() == null)
                || links.stream().map(link -> link.getItem().getId()).distinct().count() != links.size())
            return unavailable(request, "INCONSISTENT_ITEM_IDENTIFIERS");
        var specialPool = specialResolver.prepare(box, links);
        Map<String, List<CosmeticItem>> regularPools = new HashMap<>();
        Map<String, CaseItem> byExternalId = new HashMap<>();
        for (var link : links) {
            byExternalId.put(link.getItem().getExternalId(), link);
            if (!link.isSpecial()) regularPools.computeIfAbsent(link.getItem().getRarity().getId(),
                    ignored -> new ArrayList<>()).add(link.getItem());
        }
        regularPools.values().forEach(pool -> pool.sort(Comparator.comparing(CosmeticItem::getExternalId)));
        int totalWeight = distribution.stream().mapToInt(OfficialCaseOdds.RarityWeight::weight).sum();
        Map<String, Integer> rarityCounts = new HashMap<>();
        Map<String, Integer> itemCounts = new TreeMap<>();
        for (int sample = 0; sample < request.samples(); sample++) {
            var rarity = drawRarity(distribution, random.nextInt(totalWeight));
            String selected;
            if (OfficialCaseOdds.SPECIAL_GROUP.equals(rarity)) {
                selected = specialPool.resolve().externalId();
            } else {
                var pool = regularPools.get(rarity);
                selected = pool.get(random.nextInt(pool.size())).getExternalId();
            }
            rarityCounts.merge(rarity, 1, Integer::sum);
            itemCounts.merge(selected, 1, Integer::sum);
        }
        var rarities = distribution.stream().map(entry -> new RarityCount(entry.rarityId(), entry.chance().scope(),
                rarityCounts.getOrDefault(entry.rarityId(), 0),
                percent(rarityCounts.getOrDefault(entry.rarityId(), 0), request.samples()), entry.chance())).toList();
        var items = itemCounts.entrySet().stream().map(entry -> {
            var link = byExternalId.get(entry.getKey());
            var item = link.getItem();
            var rarity = item.getRarity();
            return new ItemCount(item.getId(), item.getExternalId(), item.getName(),
                    new Rarity(rarity.getId(), rarity.getName(), rarity.getColor()), link.isSpecial(), entry.getValue(),
                    percent(entry.getValue(), request.samples()),
                    link.isSpecial() ? CatalogDtos.SpecialRewardGroup.UNIFORM_RULE : "OFFICIAL_EQUAL_WITHIN_RARITY",
                    !link.isSpecial(), UNDETERMINED_VARIANT);
        }).toList();
        return new SimulationPreviewResponse("AVAILABLE", null, box.getExternalId(), request.samples(),
                request.samples(), rarities, items, LIMITATIONS);
    }

    private String drawRarity(List<OfficialCaseOdds.RarityWeight> distribution, int ticket) {
        int cumulative = 0;
        for (var entry : distribution) {
            cumulative += entry.weight();
            if (ticket < cumulative) return entry.rarityId();
        }
        throw new IllegalStateException("Distribuição estatística inconsistente.");
    }

    private BigDecimal percent(int count, int samples) {
        return BigDecimal.valueOf(count * 100L).divide(BigDecimal.valueOf(samples), 8, RoundingMode.HALF_UP);
    }

    private SimulationPreviewResponse unavailable(SimulationPreviewRequest request, String reason) {
        return new SimulationPreviewResponse("UNAVAILABLE", reason, request.caseId(), request.samples(),
                0, List.of(), List.of(), LIMITATIONS);
    }
}
