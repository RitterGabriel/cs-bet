package com.csbet.backend.inventory;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
public final class BulkSaleDtos {
    private BulkSaleDtos() { }
    public static final int MAX_ITEMS = 2000;
    public record Line(@NotNull @Positive Long inventoryId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2)
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal expectedPrice) { }
    public record Request(@NotNull UUID requestId, @NotNull @Size(min = 1, max = MAX_ITEMS) List<@NotNull @Valid Line> items) {
        @AssertTrue(message = "Não repita unidades na venda.")
        public boolean isUnique() { return items == null || items.stream().filter(Objects::nonNull).map(Line::inventoryId).distinct().count() == items.size(); }
    }
    public record Quote(List<Line> items, int skippedItems,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal total) { }
    public record Receipt(Long batchId, int soldItems,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal balance) { }
}
