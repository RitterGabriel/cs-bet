package com.csbet.backend.casecatalog;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface WeaponCaseRepository extends JpaRepository<WeaponCase, Long>, JpaSpecificationExecutor<WeaponCase> {
    Optional<WeaponCase> findBySourceAndExternalId(String source, String externalId);
    List<WeaponCase> findAllBySource(String source);
}
