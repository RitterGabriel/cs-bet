package com.csbet.backend.casecatalog;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface CustomCaseRepository extends JpaRepository<CustomCaseDefinition,Long> {
    @EntityGraph(attributePaths="box") List<CustomCaseDefinition> findAllByOrderByCaseIdAsc();
    @EntityGraph(attributePaths="box") Optional<CustomCaseDefinition> findByBoxExternalId(String externalId);
}
