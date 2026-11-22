package com.csbet.backend.inventory;

import com.csbet.backend.exception.ApiExceptionHandler.ApiError;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(-1)
@RestControllerAdvice(assignableTypes = InventoryController.class)
public class InventoryExceptionHandler {
    @ExceptionHandler({InvalidInventoryRequestException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> invalid() {
        return ResponseEntity.badRequest().body(new ApiError("Filtros ou paginação do inventário inválidos."));
    }
}
