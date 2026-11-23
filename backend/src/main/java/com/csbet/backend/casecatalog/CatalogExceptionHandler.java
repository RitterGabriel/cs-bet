package com.csbet.backend.casecatalog;

import com.csbet.backend.exception.ApiExceptionHandler.ApiError;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(-1)
@RestControllerAdvice(assignableTypes = {CaseController.class, CustomCaseController.class})
public class CatalogExceptionHandler {
    @ExceptionHandler(CatalogNotFoundException.class)
    ResponseEntity<ApiError> notFound() { return ResponseEntity.status(404).body(new ApiError("Caixa não encontrada.")); }
    @ExceptionHandler({InvalidCatalogRequestException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> invalid() { return ResponseEntity.badRequest().body(new ApiError("Filtros ou paginação inválidos.")); }
}
