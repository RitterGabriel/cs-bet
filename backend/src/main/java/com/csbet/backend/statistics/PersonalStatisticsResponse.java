package com.csbet.backend.statistics;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PersonalStatisticsResponse(Summary summary, ActivityPage activity) {
    public record Summary(
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal balance,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal creditsAdded,
            long walletMovements, long availableItems,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal inventoryEstimatedValue,
            long unpricedAvailableItems, long soldItems,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal salesTotal,
            long unvaluedSales, boolean openingHistoryAvailable, Long openedCases,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal openingSpend) { }
    public record Activity(String id, ActivityType type, OffsetDateTime occurredAt, String itemName,
                           @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
                           @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal resultingBalance) { }
    public record ActivityPage(List<Activity> content, int page, int size, long totalElements, int totalPages) { }
}
