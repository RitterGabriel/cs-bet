package com.csbet.backend.inventory;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice(assignableTypes = {InventorySaleController.class, BulkInventorySaleController.class})
@Order(-1)
public class InventorySaleExceptionHandler {
    public record SaleError(String message, String code) { }
    @ExceptionHandler(com.csbet.backend.casecatalog.CatalogNotFoundException.class)
    ResponseEntity<com.csbet.backend.exception.ApiExceptionHandler.ApiError> notFound() {
        return ResponseEntity.status(404).body(new com.csbet.backend.exception.ApiExceptionHandler.ApiError("Item não encontrado."));
    }
    @ExceptionHandler({InventorySaleException.class, IllegalArgumentException.class, org.springframework.dao.ConcurrencyFailureException.class})
    ResponseEntity<SaleError> conflict(Exception failure) {
        boolean busy = failure instanceof org.springframework.dao.ConcurrencyFailureException;
        return ResponseEntity.status(409).body(new SaleError(busy ? "Carteira ocupada. Repita a mesma tentativa." : failure.getMessage(),
                busy ? "SALE_BUSY" : "SALE_REJECTED"));
    }
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class, org.springframework.http.converter.HttpMessageNotReadableException.class})
    ResponseEntity<SaleError> invalid(Exception failure) {
        return ResponseEntity.badRequest().body(new SaleError("Informe um UUID e o preço esperado da variante.", "SALE_REJECTED"));
    }
}
