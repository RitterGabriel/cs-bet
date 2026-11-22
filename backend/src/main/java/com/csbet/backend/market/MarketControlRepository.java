package com.csbet.backend.market;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
public interface MarketControlRepository extends JpaRepository<MarketControl, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from MarketControl state where state.id = 1")
    MarketControl lockForUpdate();
}
