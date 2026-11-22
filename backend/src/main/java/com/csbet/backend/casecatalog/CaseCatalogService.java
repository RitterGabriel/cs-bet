package com.csbet.backend.casecatalog;

import java.util.Locale;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.csbet.backend.casecatalog.CatalogDtos.*;

@Service
@Transactional(readOnly = true)
public class CaseCatalogService {
    private final WeaponCaseRepository cases;
    private final CaseItemRepository contents;
    public CaseCatalogService(WeaponCaseRepository cases, CaseItemRepository contents) {
        this.cases = cases; this.contents = contents;
    }
    public CasePage list(int page, int size, String search, String category, String sort) {
        if (page < 0 || page > 100000 || size < 1 || size > 60 || search.length() > 150
                || !(category.equals("official") || category.equals("custom"))
                || !(sort.equals("name") || sort.equals("featured"))) throw new InvalidCatalogRequestException();
        String escaped = search.strip().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
        Specification<WeaponCase> filter = (root, query, cb) -> cb.and(
                cb.equal(root.get("category"), category),
                cb.like(cb.lower(root.get("name")), "%" + escaped + "%", '!'));
        Sort ordering = sort.equals("name") ? Sort.by("name", "externalId") : Sort.by(Sort.Direction.DESC, "externalId");
        var result = cases.findAll(filter, PageRequest.of(page, size, ordering));
        return new CasePage(result.getContent().stream().map(CaseSummary::from).toList(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }
    public CaseDetail detail(String externalId) {
        var box = cases.findBySourceAndExternalId(CatalogImportService.SOURCE, externalId)
                .orElseThrow(CatalogNotFoundException::new);
        var links = contents.findContents(box.getId());
        return new CaseDetail(CaseSummary.from(box),
                links.stream().filter(link -> !link.isSpecial()).map(ItemResponse::from).toList(),
                links.stream().filter(CaseItem::isSpecial).map(ItemResponse::from).toList(),
                box.getSpecialNotice(), "https://github.com/ByMykel/CSGO-API", box.getSourceRevision(), box.getImportedAt(),
                "URLs da fonte. Artes pertencem à Valve; a licença MIT dos dados não concede licença sobre essas imagens.");
    }
}
