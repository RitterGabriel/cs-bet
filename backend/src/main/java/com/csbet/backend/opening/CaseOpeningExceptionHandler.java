package com.csbet.backend.opening;

import com.csbet.backend.exception.ApiExceptionHandler.ApiError;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = {CaseOpeningController.class, OpeningBatchController.class})
@Order(-1)
public class CaseOpeningExceptionHandler {
    @ExceptionHandler(com.csbet.backend.casecatalog.CatalogNotFoundException.class)
    ResponseEntity<com.csbet.backend.exception.ApiExceptionHandler.ApiError> notFound() {
        return ResponseEntity.status(404).body(new com.csbet.backend.exception.ApiExceptionHandler.ApiError("Caixa não encontrada."));
    }
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> invalid(Exception failure) {
        return ResponseEntity.badRequest().body(new ApiError("Informe uma caixa, um identificador UUID e o preço esperado com até duas casas decimais."));
    }
    public record OpeningError(String message, String code) { }
    @ExceptionHandler({OpeningUnavailableException.class, org.springframework.dao.ConcurrencyFailureException.class})
    ResponseEntity<OpeningError> conflict(Exception failure) {
        return ResponseEntity.status(409).body(new OpeningError(failure instanceof OpeningUnavailableException
                ? failure.getMessage() : "Carteira ocupada. Repita a tentativa com o mesmo identificador.",
                failure instanceof OpeningUnavailableException ? "OPENING_REJECTED" : "OPENING_BUSY"));
    }
}
