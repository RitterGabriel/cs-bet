package com.csbet.backend.inventory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InventorySaleBatchRepository extends JpaRepository<InventorySaleBatch,Long> {
    Optional<InventorySaleBatch> findByUserIdAndRequestId(Long userId,String requestId);
}
