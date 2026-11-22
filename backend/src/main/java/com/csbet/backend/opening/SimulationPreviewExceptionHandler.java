package com.csbet.backend.opening;

import com.csbet.backend.casecatalog.CatalogNotFoundException;
import com.csbet.backend.exception.ApiExceptionHandler.ApiError;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@Order(-1)
@RestControllerAdvice(assignableTypes = SimulationPreviewController.class)
public class SimulationPreviewExceptionHandler {
    @ExceptionHandler(CatalogNotFoundException.class)
    ResponseEntity<ApiError> notFound() {
        return ResponseEntity.status(404).body(new ApiError("Caixa oficial não encontrada."));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            ConstraintViolationException.class})
    ResponseEntity<ApiError> invalid() {
        return ResponseEntity.badRequest().body(new ApiError(
                "Informe o ID da caixa e uma quantidade inteira entre 1 e " + SimulationPreviewRequest.MAX_SAMPLES + "."));
    }
}
