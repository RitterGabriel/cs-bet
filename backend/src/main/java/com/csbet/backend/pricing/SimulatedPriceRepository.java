package com.csbet.backend.pricing;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SimulatedPriceRepository extends JpaRepository<SimulatedPrice, Long> {
    List<SimulatedPrice> findBySkinIdIn(Collection<Long> ids);
}
