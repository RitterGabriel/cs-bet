package com.csbet.backend.casecatalog;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface CustomCaseEntryRepository extends JpaRepository<CustomCaseEntry,Long> {
    @Query("select distinct e from CustomCaseEntry e join fetch e.skin s join fetch s.rarity left join fetch e.variants where e.definition.caseId=:caseId order by e.id")
    List<CustomCaseEntry> findContents(@Param("caseId") Long caseId);
}
