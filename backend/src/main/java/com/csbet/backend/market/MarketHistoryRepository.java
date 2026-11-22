package com.csbet.backend.market;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MarketHistoryRepository extends JpaRepository<MarketHistory, Long> {
    Page<MarketHistory> findByVariantId(Long variantId, Pageable pageable);
}
