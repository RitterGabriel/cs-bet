package com.csbet.backend.pricing;
import com.csbet.backend.exception.ApiExceptionHandler.ApiError;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(-1)
@RestControllerAdvice(assignableTypes = PriceController.class)
public class PriceExceptionHandler {
    @ExceptionHandler(PriceNotFoundException.class)
    ResponseEntity<ApiError> missing() { return ResponseEntity.status(404).body(new ApiError("Skin ou preço simulado ainda não disponível.")); }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> invalid() { return ResponseEntity.badRequest().body(new ApiError("Paginação de preços inválida.")); }
}
