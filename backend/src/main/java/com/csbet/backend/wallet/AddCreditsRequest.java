package com.csbet.backend.wallet;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record AddCreditsRequest(
        @NotNull @DecimalMin("0.01") @DecimalMax("10000.00") @Digits(integer = 5, fraction = 2) BigDecimal amount,
        @NotNull UUID requestId) { }
