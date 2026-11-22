package com.csbet.backend.market;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MarketVariantRepository extends JpaRepository<MarketVariant, Long> {
    List<MarketVariant> findBySkinIdIn(Collection<Long> ids);
    List<MarketVariant> findByCaseIdIn(Collection<Long> ids);
}
