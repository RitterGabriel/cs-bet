package com.csbet.backend.inventory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InventorySaleRepository extends JpaRepository<InventorySale, Long> {
    Optional<InventorySale> findByUserIdAndRequestId(Long userId, String requestId);
}
