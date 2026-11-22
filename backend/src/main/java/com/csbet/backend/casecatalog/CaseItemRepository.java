package com.csbet.backend.casecatalog;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface CaseItemRepository extends JpaRepository<CaseItem, CaseItemId> {
    @Query("select link from CaseItem link join fetch link.item item join fetch item.rarity where link.weaponCase.id = :caseId order by item.name, item.externalId")
    List<CaseItem> findContents(@Param("caseId") Long caseId);
    @Modifying
    @Query("delete from CaseItem link where link.weaponCase.id = :caseId")
    void deleteContents(@Param("caseId") Long caseId);
}
