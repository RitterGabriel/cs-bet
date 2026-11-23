package com.csbet.backend.inventory;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;
public record InventorySaleRequest(@NotNull UUID requestId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) BigDecimal expectedPrice) { }
