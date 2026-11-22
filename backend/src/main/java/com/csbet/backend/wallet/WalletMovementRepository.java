package com.csbet.backend.wallet;

import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletMovementRepository extends JpaRepository<WalletMovement, Long> {
    Optional<WalletMovement> findByUserIdAndRequestId(Long userId, String requestId);
    Page<WalletMovement> findByUserId(Long userId, Pageable pageable);
}
