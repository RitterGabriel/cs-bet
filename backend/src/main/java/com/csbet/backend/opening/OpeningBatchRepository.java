package com.csbet.backend.opening;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OpeningBatchRepository extends JpaRepository<OpeningBatch, Long> {
    Optional<OpeningBatch> findByUserIdAndRequestId(Long userId, String requestId);
}
