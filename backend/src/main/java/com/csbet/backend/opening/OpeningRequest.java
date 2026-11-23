package com.csbet.backend.opening;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record OpeningRequest(@NotBlank @Size(max = 120) String caseId, @NotNull UUID requestId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) BigDecimal expectedPrice) { }
