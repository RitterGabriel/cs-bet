package com.csbet.backend.pricing;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    Page<PriceHistory> findBySkinId(Long skinId, Pageable pageable);
}
