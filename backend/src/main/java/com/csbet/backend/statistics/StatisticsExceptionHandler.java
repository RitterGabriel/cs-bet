package com.csbet.backend.statistics;

import com.csbet.backend.exception.ApiExceptionHandler.ApiError;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Order(-1)
@RestControllerAdvice(assignableTypes = PersonalStatisticsController.class)
public class StatisticsExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> invalid() {
        return ResponseEntity.badRequest().body(new ApiError("Filtros ou paginação inválidos."));
    }
}
