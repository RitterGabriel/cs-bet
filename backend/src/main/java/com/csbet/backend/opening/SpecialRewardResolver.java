package com.csbet.backend.opening;

import com.csbet.backend.casecatalog.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Segunda etapa interna da simulação: cada ID especial da caixa tem probabilidade 1/N.
 * Não representa distribuição oficial da Valve. Não cria prêmio intermediário no inventário,
 * não escolhe desgaste, não debita saldo e não possui endpoint HTTP.
 */
@Service
public class SpecialRewardResolver {
    private final WeaponCaseRepository cases;
    private final CaseItemRepository contents;
    private final OfficialCaseOdds odds;
    private final RandomGenerator random;

    @Autowired
    public SpecialRewardResolver(WeaponCaseRepository cases, CaseItemRepository contents, OfficialCaseOdds odds) {
        this(cases,contents,odds,new SecureRandom());
    }
    SpecialRewardResolver(WeaponCaseRepository cases, CaseItemRepository contents, OfficialCaseOdds odds, RandomGenerator random) {
        this.cases=cases; this.contents=contents; this.odds=odds; this.random=random;
    }
    public record ResolvedSpecialItem(Long skinId, String externalId) { }

    /** Chamar somente depois de a etapa principal selecionar o grupo especial. */
    @Transactional(readOnly=true)
    public ResolvedSpecialItem resolveSpecialWin(String caseId) {
        var box = cases.findBySourceAndExternalId("BYMYKEL",caseId).orElseThrow(CatalogNotFoundException::new);
        var links = contents.findContents(box.getId());
        return prepare(box, links).resolve();
    }

    /** Valida uma única vez o conjunto carregado pelo motor, sem consultas por amostra. */
    public PreparedSpecialPool prepare(WeaponCase box, List<CaseItem> links) {
        if (odds.specialGroupForCase(box,links) == null) throw new IllegalStateException("Grupo especial não configurado ou incompleto.");
        var pool = links.stream().filter(CaseItem::isSpecial)
                .map(CaseItem::getItem).sorted(Comparator.comparing(com.csbet.backend.skin.CosmeticItem::getExternalId)).toList();
        if (pool.isEmpty() || pool.stream().anyMatch(item -> item.getId() == null)
                || pool.stream().map(com.csbet.backend.skin.CosmeticItem::getId).distinct().count() != pool.size())
            throw new IllegalStateException("Conjunto especial inconsistente.");
        return new PreparedSpecialPool(pool.stream()
                .map(item -> new ResolvedSpecialItem(item.getId(), item.getExternalId())).toList());
    }

    public final class PreparedSpecialPool {
        private final List<ResolvedSpecialItem> items;
        private PreparedSpecialPool(List<ResolvedSpecialItem> items) { this.items = List.copyOf(items); }
        public ResolvedSpecialItem resolve() { return items.get(random.nextInt(items.size())); }
    }
}
