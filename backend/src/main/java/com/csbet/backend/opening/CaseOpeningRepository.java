package com.csbet.backend.opening;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CaseOpeningRepository extends JpaRepository<CaseOpening, Long> {
    Optional<CaseOpening> findByUserIdAndRequestId(Long userId, String requestId);
}
