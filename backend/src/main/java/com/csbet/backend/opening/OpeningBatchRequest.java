package com.csbet.backend.opening;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record OpeningBatchRequest(@NotBlank @Size(max = 120) String caseId, @NotNull UUID requestId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) BigDecimal expectedPrice, int quantity) {
    @AssertTrue(message = "Escolha 1, 2, 5 ou 10 caixas.")
    public boolean isSupportedQuantity() { return quantity == 1 || quantity == 2 || quantity == 5 || quantity == 10; }
}
