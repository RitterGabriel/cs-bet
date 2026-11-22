package com.csbet.backend.wallet;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public final class WalletResponse {
    private WalletResponse() { }
    public record Balance(String balance) { }
    public record Movement(Long id, String type, String amount, String resultingBalance, OffsetDateTime createdAt) {
        static Movement from(WalletMovement movement) {
            return new Movement(movement.getId(), movement.getType(), movement.getAmount().toPlainString(),
                    movement.getResultingBalance().toPlainString(), movement.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
    }
    public record History(List<Movement> content, int page, int totalPages, long totalElements) { }
}
