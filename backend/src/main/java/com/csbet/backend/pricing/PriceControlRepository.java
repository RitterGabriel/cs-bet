package com.csbet.backend.pricing;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

public interface PriceControlRepository extends JpaRepository<PriceControl, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select control from PriceControl control where control.id = 1")
    PriceControl lockForUpdate();
}
