package com.csbet.backend.opening;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.List;
public record OpeningBatchResponse(Long batchId, String caseId, int quantity,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal charged,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal balance, List<OpeningResponse> results) { }
